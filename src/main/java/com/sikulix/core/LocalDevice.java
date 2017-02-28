/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

import com.sikulix.api.Element;
import com.sikulix.api.Event;
import com.sikulix.api.Handler;
import com.sikulix.api.Picture;
import com.sikulix.util.animation.Animator;
import com.sikulix.util.animation.AnimatorOutQuarticEase;
import com.sikulix.util.animation.AnimatorTimeBased;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class LocalDevice extends IDevice {

  private static SXLog log = SX.getLogger("SX.Device");

  //<editor-fold desc="*** houskeeping ***">
  @Override
  public LocalDevice start(Object... args) {
    if (0 < initMonitors()) {
      try {
        robot = new LocalRobot();
      } catch (AWTException e) {
        log.terminate(1, "LocalRobot: %s", e.getMessage());
      }
    } else {
      log.terminate(1, "No monitors - might be headless %s");
    }
    if (SX.isOption("SX.withHook", false)) {
      hook = NativeHook.start();
    }
    return this;
  }

  @Override
  public void stop() {
    SX.setSXLOCALDEVICE(null);
  }

  public NativeHook getHook() {
    return hook;
  }

  NativeHook hook = null;
  
  private Object synchObject = new Object();
  private boolean locked = false;

//  private Element owner = new Element();
  private LocalRobot robot = null;

  private final int LEFT = InputEvent.BUTTON1_MASK;
  private final int MIDDLE = InputEvent.BUTTON2_MASK;
  private final int RIGHT = InputEvent.BUTTON3_MASK;

  public final double WHEEL_STEP_DELAY = 0.05;

  private double beforeButtonDown = 0;
  private double afterButtonDown = 0;
  private double beforeButtonUp = 0;
  private double afterButtonUp = 0;
  private double beforeButton = 0;
  private double afterButton = 0;
  private double clickDelay = 0;
  private double stepDelay = WHEEL_STEP_DELAY;
  private double beforeMove = 0;
  private double afterMove = 0;

  //TODO MouseButtons
  private boolean areExtraMouseButtonsEnabled = false;
  private int numberOfButtons = 0;
  private int maskForButton1 = 0;

  public void getMouseSetup() {
    areExtraMouseButtonsEnabled = Toolkit.getDefaultToolkit().areExtraMouseButtonsEnabled();
    numberOfButtons = MouseInfo.getNumberOfButtons();
    maskForButton1 = InputEvent.getMaskForButton(1);
  }

  public String toString() {
    return "LocalDevice";
  }
  //</editor-fold>

  //<editor-fold desc="*** coordinate usage">
  public synchronized void lock() {
    synchronized (synchObject) {
      while (locked) {
        try {
          synchObject.wait();
          locked = true;
          log.trace("lock start");
          checkLastPos();
          checkShouldRunCallback();
          if (shouldTerminate) {
            shouldTerminate = false;
            throw new AssertionError(String.format("Device: termination after return from callback"));
          }
        } catch (InterruptedException e) {
        }
      }
    }
  }

  public synchronized void unlock() {
    if (locked) {
      locked = false;
      synchObject.notify();
      log.trace("lock stop");
    }
  }
  //</editor-fold>

  //<editor-fold desc="*** mouse pointer location ***">
  private Element lastPos = null;

  public void setLastPos() {
    lastPos = at();
  }

  public void checkLastPos() {
    if (lastPos == null) {
      return;
    }
    Element pos = at();
    if (pos != null && (lastPos.x != pos.x || lastPos.y != pos.y)) {
      log.debug("moved externally: now (%d,%d) was (%d,%d) (movedAction %d)",
              pos.x, pos.y, lastPos.x, lastPos.y, movedAction);
      if (movedAction > 0) {
        if (MOVEDHIGHLIGHT) {
          showMousePos(pos.getPoint());
        }
      }
      if (movedAction == MOVEDPAUSE) {
        while (pos.x > 0 && pos.y > 0) {
          SX.pause(0.5);
          pos = at();
          if (MOVEDHIGHLIGHT) {
            showMousePos(pos.getPoint());
          }
        }
        if (pos.x < 1) {
          return;
        }
        SX.terminate(1, "Terminating in MouseMovedResponse = Pause");
      }
      if (movedAction == MOVEDCALLBACK) {
//TODO implement 3
//        if (mouseMovedCallback != null) {
//          mouseMovedCallback.happened(new ObserveEvent(ObserveEvent.Type.GENERIC, lastPos, new Location(pos)));
//          if (shouldTerminate) {
//            shouldTerminate = false;
//            throw new AssertionError("aborted by Sikulix.MouseMovedCallBack");
//          }
//        }
      }
    }
  }

  private void showMousePos(Point pos) {
    //TODO implement showMousePos (Element.show)
  }
  //</editor-fold>

  //<editor-fold desc="*** mouse moved reaction ***">
  public final int MOVEDIGNORE = 0;
  public final int MOVEDSHOW = 1;
  public final int MOVEDPAUSE = 2;
  public final int MOVEDCALLBACK = 3;

  private int movedAction = MOVEDIGNORE;
  private Event movedHandler;
  private boolean MOVEDHIGHLIGHT = true;

  /**
   * current setting what to do if mouse is moved outside Sikuli's mouse protection
   *
   * @return current setting see {@link #setMovedAction(int)}
   */
  public int getMovedAction() {
    return movedAction;
  }

  /**
   * what to do if mouse is moved outside Sikuli's mouse protection <br>
   * - Mouse.MOVEDIGNORE (0) ignore it (default) <br>
   * - Mouse.MOVEDSHOW (1) show and ignore it <br>
   * - Mouse.MOVEDPAUSE (2) show it and pause until user says continue <br>
   * - Mouse.MOVEDCALLBACK (3) visit a given callback {@link #setMovedCallback(Handler)} <br>
   *
   * @param movedAction value
   */
  public int setMovedAction(int movedAction) {
    if (movedAction >= MOVEDIGNORE && movedAction <= MOVEDCALLBACK) {
      this.movedAction = movedAction;
      setCallback(null);
      log.debug("setMovedAction: %d", this.movedAction);
    } else {
      this.movedAction = MOVEDIGNORE;
      log.error("setMovedAction: %d invalid - setting to MOVEDIGNORE", movedAction);
    }
    return this.movedAction;
  }

  /**
   * what to do if mouse is moved outside Sikuli's mouse protection <br>
   * in case of event the user provided callBack.generic is called
   *
   * @param handler Handler
   */
  public void setMovedCallback(Handler handler) {
    movedAction = MOVEDCALLBACK;
    movedHandler = new Event(handler);
  }

  /**
   * @param state
   */
  public void setMovedHighlight(boolean state) {
    MOVEDHIGHLIGHT = state;
  }

  /**
   * check if mouse was moved since last mouse action
   *
   * @return true/false
   */
  public boolean hasMoved() {
    Element pos = at();
    if (lastPos.x != pos.x || lastPos.y != pos.y) {
      return true;
    }
    return false;
  }
  //</editor-fold>

  //<editor-fold desc="*** click ***">
  @Override
  public Element click(Element loc) {
    return click(loc.getTarget(), Action.LEFT);
  }

  @Override
  public Element doubleClick(Element loc) {
    return click(loc, Action.LEFTDOUBLE);
  }

  @Override
  public Element rightClick(Element loc) {
    return click(loc, Action.RIGHT);
  }

  @Override
  public Element click(Action action) {
    return click(null, action);
  }

  @Override
  public Element click(Element loc, Action action) {
    if (SX.isNotNull(loc) && loc.isSpecial()) {
      return null;
    }
    boolean shouldMove = true;
    if (loc == null) {
      shouldMove = false;
      loc = at();
    }
    if (robot == null) {
      return loc;
    }
    lock();
    if (shouldMove) {
      loc = loc.getTarget();
      smoothMove(loc, robot);
    }
    log.trace("click: %s at %s%s", action, loc, (shouldMove ? " with move" : ""));
    int button = (action.toString().startsWith("L") ? LEFT : (action.toString().startsWith("R") ? RIGHT : MIDDLE));
    if (action.toString().contains("DOUBLE")) {
      SX.pause(beforeButton);
      robot.mouseDown(button);
      robot.mouseUp(button);
      robot.mouseDown(button);
      robot.mouseUp(button);
      SX.pause(afterButton);
    } else {
      SX.pause(beforeButton);
      robot.mouseDown(button);
      SX.pause(clickDelay);
      robot.mouseUp(button);
      SX.pause(afterButton);
    }
    unlock();
    return loc;
  }

  @Override
  public Element dragDrop(Element from, Element to, Object... times) {
    if (SX.isNotNull(from) && from.isSpecial()) {
      return null;
    }
    if (SX.isNotNull(to) && to.isSpecial()) {
      return null;
    }
    boolean shouldMove = true;
    if (SX.isNull(from)) {
      shouldMove = false;
      from = at();
    }
    if (SX.isNull(robot)) {
      return from;
    }
    lock();
    if (shouldMove) {
      from = from.getTarget();
      smoothMove(from, robot);
    }
    button(IDevice.Action.LEFTDOWN, false);
    if (SX.isNull(to)) {
      to = at();
    } else {
      to = to.getTarget();
      smoothMove(to, robot);
    }
    button(IDevice.Action.LEFTUP, false);
    log.trace("dragDrop %s from: %s%s to: %s", "LEFT", from, (shouldMove ? " with move" : ""), to);
    unlock();
    return to;
  }
  //</editor-fold>

  //<editor-fold desc="*** move ***">

  @Override
  public Element move(int xoff, int yoff) {
    return move(at().offset(xoff, yoff));
  }

  @Override
  public Element move(Element loc) {
    if (loc != null) {
      if (SX.isNotNull(robot)) {
        lock();
        smoothMove(loc.getTarget(), robot);
        unlock();
      }
    }
    return at();
  }

  private void smoothMove(Element dest, LocalRobot robot) {
    smoothMove(at(), dest, (long) (SX.getOptionNumber("Settings.MoveMouseDelay") * 1000L), robot);
  }

  private void smoothMove(Element src, Element dest, long ms, LocalRobot robot) {
    if (src.x == dest.x && src.y == src.y) {
      return;
    }
    int x = dest.x;
    int y = dest.y;
    log.trace("smoothMove (%.1f): (%d, %d) to (%d, %d)", (0.0 + ms)/1000, src.x, src.y, x, y);
    if (ms == 0) {
      robot.waitForIdle();
      robot.mouseMove(x, y);
      robot.waitForIdle();
    } else {
      Animator aniX = new AnimatorTimeBased(
              new AnimatorOutQuarticEase(src.x, dest.x, ms));
      Animator aniY = new AnimatorTimeBased(
              new AnimatorOutQuarticEase(src.y, dest.y, ms));
      while (aniX.running()) {
        x = (int) aniX.step();
        y = (int) aniY.step();
        robot.waitForIdle();
        robot.mouseMove(x, y);
        robot.waitForIdle();
      }
    }
    checkMouseMoved(dest);
  }

  private void checkMouseMoved(Element loc) {
    //TODO implementation with native hook
    PointerInfo mp = MouseInfo.getPointerInfo();
    Point pCurrent;
    if (mp == null) {
      log.error("checkMouseMoved: MouseInfo invalid after move to (%d, %d)", loc.x, loc.y);
    } else {
      pCurrent = mp.getLocation();
      if (pCurrent.x != loc.x || pCurrent.y != loc.y) {
        log.error("checkMouseMoved: should be (%d, %d)\nbut after move is (%d, %d)"
                        + "\nPossible cause in case you did not touch the mouse while script was running:\n"
                        + " Mouse actions are blocked generally or by the frontmost application."
                        + (SX.isWindows() ? "\nYou might try to run the SikuliX stuff as admin." : ""),
                loc.x, loc.y, pCurrent.x, pCurrent.y);
      }
    }
  }

  @Override
  public Element at() {
    PointerInfo mp = MouseInfo.getPointerInfo();
    if (mp != null) {
      return new Element(mp.getLocation());
    } else {
      log.error("MouseInfo.getPointerInfo(): null");
      return new Element();
    }
  }
  //</editor-fold>

  //<editor-fold desc="*** buttons ***">
  @Override
  public void button(Action action) {
    button(action, true);
  }

  private void button(Action action, boolean withLock) {
    int button = action.toString().startsWith("L") ? LEFT : (action.toString().startsWith("R") ? RIGHT : MIDDLE);
    if (action.toString().contains("DOWN")) {
      if (withLock) {
        lock();
      }
      SX.pause(beforeButtonDown);
      robot.mouseDown(button);
      SX.pause(afterButtonDown);
    } else {
      SX.pause(beforeButtonUp);
      robot.mouseUp(button);
      SX.pause(afterButtonUp);
      if (withLock) {
        unlock();
      }
    }
  }
  //</editor-fold>

  //TODO implement drag, drop, dragDrop

  //<editor-fold desc="*** mouse wheel ***">
  @Override
  public void wheel(Action action, int steps) {
    lock();
    int direction = action.toString().contains("DOWN") ? 1 : -1;
    for (int i = 0; i < steps; i++) {
      robot.mouseWheel(direction);
      SX.pause(stepDelay);
    }
    unlock();
  }
  //</editor-fold>

  //<editor-fold desc="*** keyboard ***">
  public class Modifier {
    public static final int CTRL = InputEvent.CTRL_MASK;
    public static final int SHIFT = InputEvent.SHIFT_MASK;
    public static final int ALT = InputEvent.ALT_MASK;
    public static final int ALTGR = InputEvent.ALT_GRAPH_MASK;
    public static final int META = InputEvent.META_MASK;
    public static final int CMD = InputEvent.META_MASK;
    public static final int WIN = 64;
  }

  public boolean hasModifier(int mods, int mkey) {
    return (mods & mkey) != 0;
  }

  public int[] toJavaKeyCode(char character) {
    //TODO implement toJavaKeyCode
    return new int[0];
  }

  public int toJavaKeyCodeFromText(String s) {
    //TODO implement toJavaKeyCodeFromText
    return 0;
  }
  //</editor-fold>

  //<editor-fold desc="monitors, capture, show">
  private GraphicsEnvironment genv = null;
  private GraphicsDevice[] gdevs;
  private Rectangle[] monitorBounds = null;
  private Rectangle rAllMonitors;
  private int mainMonitor = -1;
  private int nMonitors = 0;

  public void resetMonitors() {
    initMonitors();
  }

  private int initMonitors() {
    if (!SX.isHeadless()) {
      genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
      gdevs = genv.getScreenDevices();
      nMonitors = gdevs.length;
      if (nMonitors == 0) {
        log.terminate(1, "getMonitors: GraphicsEnvironment has no ScreenDevices");
      }
      monitorBounds = new Rectangle[nMonitors];
      rAllMonitors = null;
      Rectangle currentBounds;
      for (int i = 0; i < nMonitors; i++) {
        currentBounds = new Rectangle(gdevs[i].getDefaultConfiguration().getBounds());
        if (null != rAllMonitors) {
          rAllMonitors = rAllMonitors.union(currentBounds);
        } else {
          rAllMonitors = currentBounds;
        }
        if (currentBounds.contains(new Point())) {
          if (mainMonitor < 0) {
            mainMonitor = i;
            log.trace("getMonitors: 1#ScreenDevice %d has (0,0) --- will be primary Screen(0)", i);
          } else {
            log.trace("getMonitors: 2#ScreenDevice %d too contains (0,0)!", i);
          }
        }
        log.trace("getMonitors: Monitor %d: (%d, %d) %d x %d", i,
                currentBounds.x, currentBounds.y, currentBounds.width, currentBounds.height);
        monitorBounds[i] = currentBounds;
      }
      if (mainMonitor < 0) {
        log.trace("getMonitors: No ScreenDevice has (0,0) --- using 0 as primary: %s", monitorBounds[0]);
        mainMonitor = 0;
      }
    } else {
      log.error("running in headless environment");
    }
    return nMonitors;
  }

  @Override
  public int getNumberOfMonitors() {
    return nMonitors;
  }

  @Override
  public Rectangle getMonitor(int... ids) {
    if (SX.isHeadless() || mainMonitor < 0) {
      return new Rectangle();
    }
    if (ids.length == 1) {
      return new Rectangle(monitorBounds[(ids[0] < 0 || ids[0] >= nMonitors) ? mainMonitor : ids[0]]);
    }
    return monitorBounds[mainMonitor];
  }

  public int getMainMonitorID() {
    return mainMonitor;
  }

  public Rectangle getAllMonitors() {
    return rAllMonitors;
  }

  public Rectangle[] getMonitors() {
    return monitorBounds;
  }

  public Element getContainingMonitor(Element element) {
    for (Rectangle monitor : getMonitors()) {
      if (monitor.contains(element.x, element.y)) {
        return new Element(monitor);
      }
    }
    return new Element(monitorBounds[mainMonitor]);
  }

  public GraphicsDevice getGraphicsDevice(int id) {
    return gdevs[id];
  }
  //TODO implement more support for Retina (HiDpi)
  public static boolean isRetina(int id) {
    LocalDevice localDevice = new LocalDevice().start();
    if (SX.isMac()) {
      GraphicsDevice screen = localDevice.getGraphicsDevice(0);
//      if (screen instanceof CGraphicsDevice) {
//        return 2 == ((CGraphicsDevice) screen).getScaleFactor();
//      }
    }
    return false;
  }


  //TODO implement capture, show coordination

  public Picture capture(Element elem) {
    Picture img = new Picture(robot.createScreenCapture(elem.getRectangle()));
    if (img.hasContent()) {
      elem.setContent(img.getContent());
    } else {
      elem.setContent();
    }
    return img;
  }
  //</editor-fold>

  //<editor-fold desc="*** Callback">
  private Event callback = null;
  private boolean shouldRunCallback = false;
  private boolean shouldTerminate = false;

  public void setShouldTerminate() {
    shouldTerminate = true;
    log.debug("setShouldTerminate: request issued");
  }

  public boolean isShouldRunCallback() {
    return shouldRunCallback;
  }

  public void setShouldRunCallback(boolean shouldRun) {
    shouldRunCallback = shouldRun;
  }

  private void checkShouldRunCallback() {
    if (shouldRunCallback && callback != null) {
      callback.handle();
      if (shouldTerminate) {
        shouldTerminate = false;
        throw new AssertionError("aborted by Sikulix.GenericDeviceCallBack");
      }
    }
  }

  /**
   * set a callback function for handling Device events <br>
   * in case of event the user provided callBack.happened is called
   *
   * @param handler
   */
  public void setCallback(Handler handler) {
    callback = new Event(handler);
  }
  //</editor-fold>

  enum KeyMode {
    PRESS_ONLY, RELEASE_ONLY, PRESS_RELEASE
  };

  private class LocalRobot extends Robot {

    //<editor-fold desc="housekeeping">
    final int MAX_DELAY = 60000;
    private int heldButtons = 0;
    private String heldKeys = "";
    private final ArrayList<Integer> heldKeyCodes = new ArrayList<Integer>();
    public int stdAutoDelay = 0;
    public int stdDelay = 10;

    public LocalRobot() throws AWTException {
      super();
      setAutoDelay(stdAutoDelay);
      setAutoWaitForIdle(false);
    }
    //</editor-fold>

    //<editor-fold desc="Mouse">
    public void mouseDown(int buttons) {
      if (heldButtons != 0) {
        log.error("mouseDown: buttons still pressed - using all", buttons, heldButtons);
        heldButtons |= buttons;
      } else {
        heldButtons = buttons;
      }
      doMouseDown(heldButtons);
    }

    private void doMouseDown(int buttons) {
      Element.fakeHighlight(true);
      pause(100);
      Element.fakeHighlight(false);
      pause(100);
      waitForIdle();
      mousePress(buttons);
      if (stdAutoDelay == 0) {
        pause(stdDelay);
      }
      waitForIdle();
    }

    public int mouseUp(int buttons) {
      if (buttons == 0) {
        doMouseUp(heldButtons);
        heldButtons = 0;
      } else {
        doMouseUp(buttons);
        heldButtons &= ~buttons;
      }
      return heldButtons;
    }

    private void doMouseUp(int buttons) {
      waitForIdle();
      mouseRelease(buttons);
      if (stdAutoDelay == 0) {
        pause(stdDelay);
      }
      waitForIdle();
    }

    public void pause(int ms) {
      if (ms < 0) {
        return;
      }
      while (ms > MAX_DELAY) {
        super.delay(MAX_DELAY);
        ms -= MAX_DELAY;
      }
      super.delay(ms);
    }
    //</editor-fold>

    //<editor-fold desc="Screen">
    public Picture captureScreen(Rectangle rect) {
      waitForIdle();
      BufferedImage bImg = createScreenCapture(rect);
      waitForIdle();
      log.trace("captureScreen: [%d,%d, %dx%d]",
              rect.x, rect.y, rect.width, rect.height);
      return new Picture(bImg);
    }

    public Color getColorAt(int x, int y) {
      return getPixelColor(x, y);
    }
    //</editor-fold>

    //<editor-fold desc="Keys">
    public void pressModifiers(int modifiers) {
      if (hasModifier(modifiers, Modifier.SHIFT)) {
        doKeyPress(KeyEvent.VK_SHIFT);
      }
      if (hasModifier(modifiers, Modifier.CTRL)) {
        doKeyPress(KeyEvent.VK_CONTROL);
      }
      if (hasModifier(modifiers, Modifier.ALT)) {
        doKeyPress(KeyEvent.VK_ALT);
      }
      if (hasModifier(modifiers, Modifier.META)) {
        if (SX.isWindows()) {
          doKeyPress(KeyEvent.VK_WINDOWS);
        } else {
          doKeyPress(KeyEvent.VK_META);
        }
      }
    }

    public void releaseModifiers(int modifiers) {
      if (hasModifier(modifiers, Modifier.SHIFT)) {
        doKeyRelease(KeyEvent.VK_SHIFT);
      }
      if (hasModifier(modifiers, Modifier.CTRL)) {
        doKeyRelease(KeyEvent.VK_CONTROL);
      }
      if (hasModifier(modifiers, Modifier.ALT)) {
        doKeyRelease(KeyEvent.VK_ALT);
      }
      if (hasModifier(modifiers, Modifier.META)) {
        if (SX.isWindows()) {
          doKeyRelease(KeyEvent.VK_WINDOWS);
        } else {
          doKeyRelease(KeyEvent.VK_META);
        }
      }
    }

    public void keyDown(String keys) {
      if (keys != null && !"".equals(keys)) {
        for (int i = 0; i < keys.length(); i++) {
          if (heldKeys.indexOf(keys.charAt(i)) == -1) {
            log.trace("press: " + keys.charAt(i));
            typeChar(keys.charAt(i), KeyMode.PRESS_ONLY);
            heldKeys += keys.charAt(i);
          }
        }
      }
    }

    public void keyDown(int code) {
      if (!heldKeyCodes.contains(code)) {
        doKeyPress(code);
        heldKeyCodes.add(code);
      }
    }

    private void doKeyPress(int keyCode) {
      waitForIdle();
      keyPress(keyCode);
      if (stdAutoDelay == 0) {
        pause(stdDelay);
      }
      waitForIdle();
    }

    public void keyUp(String keys) {
      if (keys != null && !"".equals(keys)) {
        for (int i = 0; i < keys.length(); i++) {
          int pos;
          if ((pos = heldKeys.indexOf(keys.charAt(i))) != -1) {
            log.trace("release: " + keys.charAt(i));
            typeChar(keys.charAt(i), KeyMode.RELEASE_ONLY);
            heldKeys = heldKeys.substring(0, pos)
                    + heldKeys.substring(pos + 1);
          }
        }
      }
    }

    public void keyUp(int code) {
      if (heldKeyCodes.contains(code)) {
        doKeyRelease(code);
        heldKeyCodes.remove((Object) code);
      }
    }

    public void keyUp() {
      keyUp(heldKeys);
      for (int code : heldKeyCodes) {
        keyUp(code);
      }
    }

    private void doKeyRelease(int keyCode) {
      waitForIdle();
      keyRelease(keyCode);
      if (stdAutoDelay == 0) {
        pause(stdDelay);
      }
      waitForIdle();
    }
    //</editor-fold>

    //<editor-fold desc="type">
    public void typeChar(char character, KeyMode mode) {
      log.trace("Robot: doType: %s ( %d )",
              KeyEvent.getKeyText(toJavaKeyCode(character)[0]),
              toJavaKeyCode(character)[0]);
      doType(mode, toJavaKeyCode(character));
    }

    public void typeKey(int key) {
      log.trace("Robot: doType: %s ( %d )", KeyEvent.getKeyText(key), key);
      if (SX.isMac()) {
        if (key == toJavaKeyCodeFromText("#N.")) {
          doType(KeyMode.PRESS_ONLY, toJavaKeyCodeFromText("#C."));
          doType(KeyMode.PRESS_RELEASE, key);
          doType(KeyMode.RELEASE_ONLY, toJavaKeyCodeFromText("#C."));
          return;
        } else if (key == toJavaKeyCodeFromText("#T.")) {
          doType(KeyMode.PRESS_ONLY, toJavaKeyCodeFromText("#C."));
          doType(KeyMode.PRESS_ONLY, toJavaKeyCodeFromText("#A."));
          doType(KeyMode.PRESS_RELEASE, key);
          doType(KeyMode.RELEASE_ONLY, toJavaKeyCodeFromText("#A."));
          doType(KeyMode.RELEASE_ONLY, toJavaKeyCodeFromText("#C."));
          return;
        } else if (key == toJavaKeyCodeFromText("#X.")) {
          key = toJavaKeyCodeFromText("#T.");
          doType(KeyMode.PRESS_ONLY, toJavaKeyCodeFromText("#A."));
          doType(KeyMode.PRESS_RELEASE, key);
          doType(KeyMode.RELEASE_ONLY, toJavaKeyCodeFromText("#A."));
          return;
        }
      }
      doType(KeyMode.PRESS_RELEASE, key);
    }

    private void doType(KeyMode mode, int... keyCodes) {
      waitForIdle();
      if (mode == KeyMode.PRESS_ONLY) {
        for (int i = 0; i < keyCodes.length; i++) {
          doKeyPress(keyCodes[i]);
        }
      } else if (mode == KeyMode.RELEASE_ONLY) {
        for (int i = 0; i < keyCodes.length; i++) {
          doKeyRelease(keyCodes[i]);
        }
      } else {
        for (int i = 0; i < keyCodes.length; i++) {
          doKeyPress(keyCodes[i]);
        }
        for (int i = 0; i < keyCodes.length; i++) {
          doKeyRelease(keyCodes[i]);
        }
      }
      waitForIdle();
    }
    //</editor-fold>
  }
}
