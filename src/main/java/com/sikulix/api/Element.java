/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.sikuli.script.Screen;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.List;

public class Element implements Comparable<Element> {

  eType eClazz = eType.ELEMENT;
  private static SXLog log = SX.getLogger("SX.ELEMENT");

  public eType getType() {
    return eClazz;
  }

  //<editor-fold desc="old API">
  private boolean throwException = SX.isOption("Settings.ThrowException");

  public boolean getThrowException() {
    return throwException;
  }

  public void setThrowException(boolean throwException) {
    this.throwException = throwException;
  }

  private float waitScanRate = (float) SX.getOptionNumber("Settings.WaitScanRate");

  public float getWaitScanRate() {
    return waitScanRate;
  }

  public void setWaitScanRate(float waitScanRate) {
    this.waitScanRate = waitScanRate;
  }

  private float observeScanRate = (float) SX.getOptionNumber("Settings.ObserveScanRate");

  public float getObserveScanRate() {
    return observeScanRate;
  }

  public void setObserveScanRate(float observeScanRate) {
    this.observeScanRate = observeScanRate;
  }

  private int repeatWaitTime = (int) SX.getOptionNumber("Settings.RepeatWaitTime");

  public int getRepeatWaitTime() {
    return repeatWaitTime;
  }

  public void setRepeatWaitTime(int repeatWaitTime) {
    this.repeatWaitTime = repeatWaitTime;
  }

  private Screen scr;

  public Screen getScreen() {
    return scr;
  }

  protected void initScreen(Screen screen) {
    if (screen != null) {
      if (screen.getID() < 0) {
        setSpecial();
        scr = screen;
        return;
      }
    }
    scr = getDevice().getContainingScreen(this);
  }
  //</editor-fold>

  //<editor-fold desc="***** construction, info">
  public String getName() {
    if (SX.isNotSet(name)) {
      if (isPoint()) {
        setName(String.format("%s_%d_%d", getTypeFirstLetter(), x, y));
      } else {
        setName(String.format("%s_%d_%d_%dx%d", getTypeFirstLetter(), x, y, w, h));
      }
    }
    return name;
  }

  public String getTypeFirstLetter() {
    return getType().toString().substring(0, 1);
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean hasName() {
    return SX.isSet(name);
  }

  private String name = "";

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }

  public int getW() {
    return w;
  }

  public int getH() {
    return h;
  }

  public long getPixelSize() {
    return w * h;
  }

  public Integer x = 0;
  public Integer y = 0;
  public Integer w = -1;
  public Integer h = -1;


  protected void copy(Element elem) {
    x = elem.x;
    y = elem.y;
    w = elem.w;
    h = elem.h;
    elementDevice = elem.elementDevice;
  }

  protected void init(int _x, int _y, int _w, int _h) {
    x = _x;
    y = _y;
    w = _w;
    h = _h;
    if (isPoint()) {
      w = _w < 0 ? 0 : _w;
      h = _h < 0 ? 0 : _h;
    }
  }

  protected void init(int[] rect) {
    int _x = 0;
    int _y = 0;
    int _w = 0;
    int _h = 0;
    switch (rect.length) {
      case 0:
        return;
      case 1:
        _x = rect[0];
        break;
      case 2:
        _x = rect[0];
        _y = rect[1];
        break;
      case 3:
        _x = rect[0];
        _y = rect[1];
        _w = rect[2];
        break;
      default:
        _x = rect[0];
        _y = rect[1];
        _w = rect[2];
        _h = rect[3];
    }
    init(_x, _y, _w, _h);
  }

  protected void init(Rectangle rect) {
    init(rect.x, rect.y, rect.width, rect.height);
  }

  protected void init(Point p) {
    init(p.x, p.y, 0, 0);
  }

  public static Element create(Object... args) {
    Element element = new Element();
    if (args.length > 0) {

    }
    return element;
  }

  public Element() {
  }

  public Element(int x, int y, int w, int h) {
    init(x, y, w, h);
  }

  public Element(int x, int y, int wh) {
    init(x, y, wh, wh);
  }

  public Element(int x, int y) {
    init(x, y, 0, 0);
  }

  public Element(int[] rect) {
    init(rect);
  }

  public Element(Rectangle rect) {
    init(rect);
  }

  public Element(Point p) {
    init(p);
  }

  public Element(Dimension dim) {
    init(0, 0, (int) dim.getWidth(), (int) dim.getHeight());
  }

  public Element(Element elem) {
    copy(elem);
  }

  public Element(Element elem, int xOffset, int yOffset) {
    copy(elem);
    x += xOffset;
    y += yOffset;
  }

  public Element(Element elem, double score) {
    this(elem);
    setScore(score);
  }

  public Element(Element elem, double score, Element off) {
    copy(elem);
    setScore(score);
    setTarget(off);
  }

  public Element(Element elem, Element off) {
    copy(elem);
    setTarget(off);
  }

  public Element(int id) {
    if (id < 0) {
      // hack: special for even margin all sides and for onChange()
      init(-id, -id, -id, -id);
    } else {
      Rectangle rect = getDevice().getMonitor(id);
      init(rect.x, rect.y, rect.width, rect.height);
    }
  }

  public Element(JSONObject jElement) {
    init(jElement);
  }

  private void init(JSONObject jElement) {
    if (jElement.has("type") && "ELEMENT".equals(jElement.getString("type"))) {
      x = jElement.optInt("x", 0);
      y = jElement.optInt("y", 0);
      h = jElement.optInt("h", -1);
      w = jElement.optInt("w", -1);
      score = jElement.optDouble("score", -1);
      if (!jElement.isNull("name")) {
        name = jElement.getString("name");
      }
      if (!jElement.isNull("lastMatch")) {
        lastMatch = new Element();
        JSONObject jLastMatch = jElement.getJSONObject("lastMatch");
        lastMatch.x = jLastMatch.optInt("x", 0);
        lastMatch.y = jLastMatch.optInt("y", 0);
        lastMatch.h = jLastMatch.optInt("h", -1);
        lastMatch.w = jLastMatch.optInt("w", -1);
        lastMatch.score = jLastMatch.optDouble("score", 0);
      }
    } else {
      log.error("new (JSONObject jElement): not super-type ELEMENT: %s", jElement);
    }
  }

  public Element(String possibleJSON) {
    this();
    try {
      JSONObject jElement = new JSONObject(possibleJSON);
      init(jElement);
    } catch (JSONException jEx) {
      log.error("new (String possibleJSON): not valid JSON: %s", jEx.getMessage());
    }
  }

//  public Element(Core.MinMaxLocResult mMinMax, Target target, Rect rect) {
//    init((int) mMinMax.maxLoc.x + target.getTarget().x +
//                    rect.x, (int) mMinMax.maxLoc.y + target.getTarget().y + rect.y,
//            target.w, target.h);
//    setScore(mMinMax.maxVal);
//  }

  public boolean isMatch() {
    return score > -1;
  }

  @Override
  public String toString() {
    if (isPoint()) {
      return String.format("[\"%s\", [%d, %d]]", getName(), x, y);
    }
    return String.format("[\"%s\", [%d, %d, %d, %d]%s]", getName(), x, y, w, h, toStringPlus());
  }

  protected String toStringPlus() {
    if (isMatch()) {
      return " %" + score * 100;
    }
    return "";
  }

  public String logString() {
    return String.format("[%d,%d %dx%d]", x, y, w, h);
  }
  //</editor-fold>

  //<editor-fold desc="***** JSON">
  public String toJSON() {
    ElementFlat elementFlat = new ElementFlat(this);
    JSONObject jElementFlat = new JSONObject(elementFlat);
    return toJSONplus(jElementFlat).toString();
  }

  protected JSONObject toJSONplus(JSONObject jElementFlat) {
    return jElementFlat;
  }

  public static class ElementFlat {

    int x = 0;
    int y = 0;
    Integer w = 0;
    Integer h = 0;

    ElementFlat lastMatch = null;
    Double score = null;

    int[] target = null;

    Element.eType clazz = Element.eType.ELEMENT;

    String name = null;
    

    public ElementFlat(Element element) {
      clazz = element.getType();
      String clazz1 = clazz.toString().substring(0, 1) + "_";
      x = element.x;
      y = element.y;
      w = element.w < 1 ? null : element.w;
      h = element.h < 1 ? null : element.h;
      if (element.hasName()) {
        name = element.getName();
        if (name.startsWith(clazz1)) {
          name = null;
        }
      }
      if (element.getScore() > 0) {
        score = element.getScore();
      }
      target = new int[]{element.getTarget().x, element.getTarget().y};
      if (element.isRectangle() && !element.isMatch()) {
        if (element.hasMatch()) {
          Element match = element.getLastMatch();
          lastMatch = new ElementFlat(match);
          lastMatch.score = match.getScore();
          lastMatch.target = new int[]{match.getTarget().x, match.getTarget().y};
        }
      }
    }
    
    public String getType() {
      return clazz.toString();
    }

    public int getX() {
      return x;
    }

    public int getY() {
      return y;
    }

    public int getW() {
      return w;
    }

    public int getH() {
      return h;
    }

    public String getName() { return name; }

    public ElementFlat getLastMatch() {
      return lastMatch;
    }

    public Double getScore() {
      return score;
    }

    public int[] getTarget() {
      return target;
    }
  }
  //</editor-fold>

  //<editor-fold desc="***** get...">
  public Element getRegion() {
    return new Element(x, y, w, h);
  }

  public Rectangle getRectangle() {
    return new Rectangle(x, y, w, h);
  }

  public void setRectangle(Rectangle rect) {
    x = rect.x;
    y = rect.y;
    w = rect.width;
    h = rect.height;
  }

  public Element getCenter() {
    return new Element(x + w / 2, y + h / 2);
  }

  public void setCentered() {
    Element centered = getCentered(new Element(SX.getSXLOCALDEVICE().getMonitor()));
    x = centered.x;
    y = centered.y;
  }

  public void setCentered(Element base) {
    Element centered = getCentered(base);
    x = centered.x;
    y = centered.y;
  }

  public Element getCentered(int... args) {
    if (args.length == 0) {
      return getCentered(new Element(SX.getSXLOCALDEVICE().getMonitor()), null);
    } else {
      return getCentered(new Element(SX.getSXLOCALDEVICE().getMonitor()), new Element(-args[0]));
    }
  }

  public Element getCentered(Element base) {
    return getCentered(base, null);
  }

  public Element getCentered(Element base, Element margin) {
    int mt = 0;
    int mr = 0;
    int mb = 0;
    int ml = 0;
    if (SX.isNotNull(margin)) {
      mt = margin.x;
      mr = margin.y;
      mb = margin.w;
      ml = margin.h;
    }
    int bcx = base.getCenter().x;
    int bcy = base.getCenter().y;
    int offX = w / 2 + ml;
    int offY = h / 2 + mt;
    int cx = bcx - offX;
    int cy = bcy - offY;
    return new Element(cx, cy);
  }

  public Point getPoint() {
    return new Point(getCenter().x, getCenter().y);
  }

  /**
   * creates a point at the given offset, might be negative<br>
   * for a rectangle the reference is the center
   *
   * @param off an offset
   * @return new location
   */
  public Element offset(Element off) {
    return new Element(getCenter().x + off.x, getCenter().y + off.y);
  }

  /**
   * creates a point at the given offset, might be negative<br>
   * for a rectangle the reference is the center
   *
   * @param xoff x offset
   * @param yoff y offset
   * @return new location
   */
  public Element offset(Integer xoff, Integer yoff) {
    return new Element(getCenter().x + xoff, getCenter().y + yoff);
  }

  public Element getTopLeft() {
    return new Element(x, y);
  }

  public Element getTopRight() {
    return new Element(x + w, y);
  }

  public Element getBottomRight() {
    return new Element(x + w, y + h);
  }

  public Element getBottomLeft() {
    return new Element(x, y + h);
  }

  //<editor-fold desc="right">
  public Element rightAt() {
    return new Element(x + w, y + h / 2);
  }

  public Element rightAt(int xoff) {
    return new Element(rightAt().x + xoff, rightAt().y);
  }

  public Element right() {
    return right(getDevice().getContainingMonitor(this).w);
  }

  public Element right(int xoff) {
    Element monitor = getDevice().getContainingMonitor(this);
    int newX = xoff < 0 ? x + w + xoff : x + w;
    return monitor.intersection(new Element(newX, y, Math.abs(xoff), h));
  }
  //</editor-fold>

  //<editor-fold desc="left">
  public Element leftAt() {
    return new Element(x, y + h / 2);
  }

  public Element leftAt(int xoff) {
    return new Element(leftAt().x - xoff, leftAt().y);
  }

  public Element left() {
    return left(getDevice().getContainingMonitor(this).w);
  }

  public Element left(int xoff) {
    Element monitor = getDevice().getContainingMonitor(this);
    return monitor.intersection(new Element(x - xoff, y, Math.abs(xoff), h));
  }
  //</editor-fold>

  public Element aboveAt() {
    return new Element(x + w / 2, y);
  }

  public Element aboveAt(int xoff) {
    return new Element(aboveAt().x, aboveAt().y - xoff);
  }

  public Element above() {
    return above(getDevice().getContainingMonitor(this).h);
  }

  public Element above(int xoff) {
    Element monitor = getDevice().getContainingMonitor(this);
    return monitor.intersection(new Element(x , y - xoff, w, Math.abs(xoff)));
  }

  public Element belowAt() {
    return new Element(x + w / 2, y + h);
  }

  public Element belowAt(int xoff) {
    return new Element(belowAt().x, belowAt().y + xoff);
  }

  public Element below() {
    return below(getDevice().getContainingMonitor(this).h);
  }

  public Element below(int xoff) {
    Element monitor = getDevice().getContainingMonitor(this);
    return monitor.intersection(new Element(x , y + h + xoff, w, Math.abs(xoff)));
  }

// TODO getColor() implement more support and make it useable

  /**
   * Get the color at the given Point (center of element) for details: see java.awt.Robot and ...Color
   *
   * @return The Color of the Point or null if not possible
   */
  public Color getColor() {
    if (isOnScreen()) {
      return getScreenColor();
    }
    return null;
  }

  private static Color getScreenColor() {
    return null;
  }
  //</editor-fold>

  //<editor-fold desc="TODO equals/compare">
  @Override
  public boolean equals(Object oThat) {
    if (this == oThat) {
      return true;
    }
    if (!(oThat instanceof Element)) {
      return false;
    }
    Element that = (Element) oThat;
    return x.equals(that.x) && y.equals(that.y) && w.equals(that.w) && h.equals(that.h);
  }

  public int compareTo(Element elem) {
    if (equals(elem)) {
      return 0;
    }
    if (elem.x > x) {
      return -1;
    } else if (elem.x == x && elem.y > y) {
      return -1;
    }
    return 1;
  }
  //</editor-fold>

  //<editor-fold desc="***** move, grow">
  private static int growDefault = 2;

  public Element grow() {
    return grow(growDefault);
  }

  public Element grow(int margin) {
    return grow(margin, margin);
  }

  public Element grow(int hori, int verti) {
    Rectangle r = getRectangle();
    r.grow(hori, verti);
    return new Element(r);
  }

  public void at(Integer x, Integer y) {
    this.x = x;
    this.y = y;
    if (!SX.isNull(target)) {
      target.translate(x - this.x, y - this.y);
    }
  }

  public void at(Element elem) {
    at(elem.x, elem.y);
  }

  public void translate(Integer xoff, Integer yoff) {
    this.x += xoff;
    this.y += yoff;
    if (!SX.isNull(target)) {
      target.translate(xoff, yoff);
    }
  }

  public void translate(Element off) {
    translate(off.x, off.y);
  }

  public void change(Element elem) {
    x = elem.x;
    y = elem.y;
    w = elem.w;
    h = elem.h;
  }
  //</editor-fold>

  //<editor-fold desc="***** combine">
  public Element union(Element elem) {
    Rectangle r1 = new Rectangle(x, y, w, h);
    Rectangle r2 = new Rectangle(elem.x, elem.y, elem.w, elem.h);
    return new Element(r1.union(r2));
  }

  public Element intersection(Element elem) {
    Rectangle r1 = new Rectangle(x, y, w, h);
    Rectangle r2 = new Rectangle(elem.x, elem.y, elem.w, elem.h);
    return new Element(r1.intersection(r2));
  }

  public void intersect(Element elem) {
    Element inter = intersection(elem);
    change(inter);
  }

  public boolean contains(Element elem) {
    if (!isRectangle() || (!elem.isRectangle() && !elem.isPoint())) {
      return false;
    }
    Rectangle r1 = new Rectangle(x, y, w, h);
    Rectangle r2 = elem.getRectangle();
    if (elem.isRectangle()) {
      return r1.contains(r2);
    } else {
      return r1.contains(elem.x, elem.y);
    }
  }
  //</editor-fold>

  //<editor-fold desc="***** target">

  public Element getLastTarget() {
    return lastTarget;
  }

  public void setLastTarget(Element lastTarget) {
    this.lastTarget = lastTarget;
  }

  private Element lastTarget = null;

  private Element target = null;

  public void setTarget(Element elem) {
    target = elem.getCenter();
  }

  public void setTarget(int x, int y) {
    target = getCenter().offset(x, y);
  }

  public void setTarget(int[] pos) {
    target = getCenter().offset(new Element(pos));
  }

  public Element getTarget() {
    if (SX.isNull(target)) {
      target = getCenter();
    }
    return target;
  }
  //</editor-fold>

  //<editor-fold desc="***** score">
  public double getScore() {
    return score;
  }

  public void setScore(double score) {
    this.score = score;
  }

  private double score = -1;

  public double getWantedScore() {
    return wantedScore;
  }

  public void setWantedScore(double wantedScore) {
    this.wantedScore = wantedScore;
  }

  private double wantedScore = -1;

  //</editor-fold>

  //<editor-fold desc="***** lastMatch">
  public Element getLastSeen() {
    if (SX.isNull(lastSeen)) {
      return new Element();
    }
    return lastSeen;
  }

  public void setLastSeen(Element lastSeen) {
    this.lastSeen = lastSeen;
  }

  private Element lastSeen = null;

  private Element lastMatch = null;
  private Element lastVanish = null;
  private java.util.List<Element> lastMatches = new ArrayList<Element>();
  private int matchIndex = -1;

  public void resetMatches() {
    lastMatch = null;
    lastMatches = new ArrayList<Element>();
    matchIndex = -1;
    lastScores = new double[]{0, 0, 0};
  }

  public boolean hasMatch() {
    return SX.isNotNull(lastMatch);
  }

  public boolean hasVanish() {
    return SX.isNotNull(lastVanish);
  }

  public boolean hasMatches() {
    return lastMatches.size() > 0;
  }

  public Element getLastMatch() {
    if (SX.isNotNull(lastMatch)) {
      return lastMatch;
    }
    return getTarget();
  }

  public Element getLastVanish() {
    return lastVanish;
  }

  public java.util.List<Element> getLastMatches() {
    return lastMatches;
  }

  public void setLastMatch(Element match) {
    lastMatch = match;
  }

  public void setLastVanish(Element match) {
    if (SX.isNotNull(match)) {
      lastMatch = null;
      lastVanish = match;
    }
  }

  private double[] lastScores = new double[]{0, 0, 0};

  public void setLastScores(double[] scores) {
    for (int i = 0; i < scores.length; i++) {
      lastScores[i] = scores[i];
    }
  }

  public double[] getLastScores() {
    return lastScores;
  }

  public void setLastMatches(java.util.List<Element> lastMatches) {
    this.lastMatches = lastMatches;
  }

  public int getMatchIndex() {
    return matchIndex;
  }

  public void setMatchIndex(int matchIndex) {
    this.matchIndex = matchIndex;
  }
  //</editor-fold>

  //<editor-fold desc="TODO  be like Selenium">
  public Element findElement(By by) {
    return new Element();
  }

  public List<Element> findElements(By by) {
    return new ArrayList<Element>();
  }

  public String getAttribute(String key) {
    return "NotAvailable";
  }

  public Element getLocation() {
    return getTopLeft();
  }

//  public Element getRect() {
//    return this;
//  }

  public Dimension getSize() {
    return new Dimension(w, h);
  }

  //TODO implement OCR
  public String getText() {
    return "NotImplemented";
  }

  public boolean isDisplayed() {
    return true;
  }

  public void sendKeys(CharSequence keys) {
    write(keys.toString());
  }
  //</editor-fold>

  //<editor-fold desc="***** variants">
  public enum eType {
    ELEMENT, SYMBOL, PICTURE, TARGET, WINDOW,
    REGION, MATCH, SCREEN, LOCATION, PATTERN, IMAGE;

    static eType isType(String strType) {
      for (eType t : eType.values()) {
        if (t.toString().equals(strType)) {
          return t;
        }
      }
      return null;
    }
  }

  public boolean isOnScreen() {
    return isElement() && !isTarget() || isWindow();
  }

  public boolean isRectangle() {
    return (isElement() && !isPoint()) || isWindow() || isRegion();
  }

  public boolean isRegion() {
    return eType.REGION.equals(getType()) || eType.SCREEN.equals(getType()) ||
            eType.MATCH.equals(getType());
  }

  public boolean isPoint() {
    return (isElement() && w < 2 && h < 2) || eType.LOCATION.equals(getType());
  }

  public boolean isElement() {
    return eType.SYMBOL.equals(getType()) || eType.ELEMENT.equals(getType()) || isPicture() || isTarget() || isWindow();
  }

  public boolean isPicture() {
    return eType.PICTURE.equals(getType());
  }

  public boolean isSymbol() {
    return eType.SYMBOL.equals(getType());
  }

  public boolean isTarget() {
    return eType.TARGET.equals(getType()) || isPicture();
  }

  public boolean isWindow() {
    return eType.WINDOW.equals(getType());
  }

  public boolean isSpecial() {
    return special;
  }

  public void setSpecial(boolean special) {
    this.special = special;
  }

  public void setSpecial() {
    this.special = true;
  }

  private boolean special = false;

  /**
   * @return true if the element is useable and/or has valid content
   */
  public boolean isValid() {
    return w > 1 && h > 1;
  }
  //</editor-fold>

  //<editor-fold desc="***** device related">
  public IDevice getDevice() {
    if (SX.isNull(elementDevice)) {
      if (!isSpecial()) {
        elementDevice = SX.getSXLOCALDEVICE();
      } else {
        log.error("not implemented: non-local devices");
        elementDevice = SX.getSXLOCALDEVICE();
      }
    }
    return elementDevice;
  }

  public void setDevice(IDevice elementDevice) {
    this.elementDevice = elementDevice;
    setSpecial();
  }

  protected IDevice elementDevice = null;

  /**
   * returns -1, if outside of any screen <br>
   *
   * @return the sequence number of the screen, that contains the given point
   */
  public int isOn() {
    Rectangle r;
    for (int i = 0; i < getDevice().getNumberOfMonitors(); i++) {
      r = getDevice().getMonitor(i);
      if (r.contains(this.x, this.y)) {
        return i;
      }
    }
    return -1;
  }
  //</editor-fold>

  //<editor-fold desc="***** content">
  protected URL urlImg = null;

  public BufferedImage get() {
    return getBufferedImage(getContent());
  }

  public Mat getContent() {
    return content;
  }

  public Mat getContent(Element elem) {
    return content.submat(new Rect(elem.x, elem.y, elem.w, elem.h));
  }

  public void setContent(Mat content) {
    this.content = content;
  }

  public Element setContent() {
    content = getNewMat();
    return this;
  }

  public static Mat getNewMat() {
    SX.loadNative(SX.NATIVES.OPENCV);
    return new Mat();
  }

  public Mat getResizedMat(double factor) {
    Mat newMat = getContent();
    if (isValid()) {
      newMat = getNewMat();
      Size newS = new Size(w * factor, h * factor);
      Imgproc.resize(getContent(), newMat, newS, 0, 0, Imgproc.INTER_AREA);
    }
    return newMat;
  }

  public boolean hasContent() {
    return SX.isNotNull(content) && !content.empty();
  }

  private Mat content = null;

  public Element load() {
    capture();
    return this;
  }

  public boolean save(String name) {
    return save(name, Picture.getBundlePath());
  }

  public boolean save(String name, String path) {
    URL url = Content.makeURL(new File(path, name).getAbsolutePath());
    if (SX.isNull(url)) {
      return false;
    }
    try {
      url = Content.makeURL(new File(path, name).getCanonicalPath());
      return save(url, name);
    } catch (IOException e) {
    }
    log.error("save: invalid: %s / %s", path, name);
    return false;
  }

  public boolean save(String name, URL urlPath) {
    URL url = Content.makeURL(urlPath, name);
    if (SX.isNotNull(url)) {
      return save(url, name);
    } else {
      log.error("save: invalid: %s / %s", urlPath, name);
    }
    return false;
  }

  public boolean save(URL url, String name) {
    if (!hasContent()) {
      load();
    }
    urlImg = null;
    if (SX.isNotNull(url) && hasContent()) {
      if ("file".equals(url.getProtocol())) {
        log.trace("save: %s", url);
        String imgFileName = SX.getValidImageFilename(url.getPath());
        Mat imgContent = getContent();
        if (Imgcodecs.imwrite(imgFileName, imgContent)) {
          urlImg = url;
          setName(name);
          return true;
        }
      } else {
        //TODO save: http and jar
        log.error("save: not implemented: %s", url);
      }
    }
    return false;
  }

  protected boolean plainColor = false;
  protected boolean blackColor = false;
  protected boolean whiteColor = false;

  public boolean isPlainColor() {
    return isValid() && plainColor;
  }

  public boolean isBlack() {
    return isValid() && blackColor;
  }

  public boolean isWhite() {
    return isValid() && blackColor;
  }

  public double getResizeFactor() {
    return isValid() ? resizeFactor : 1;
  }

  protected double resizeFactor;

  protected final static String PNG = "png";
  protected final static String dotPNG = "." + PNG;

  protected static Mat makeMat(BufferedImage bImg) {

    Mat aMat = getNewMat();
    if (bImg.getType() == BufferedImage.TYPE_INT_RGB) {
      log.trace("makeMat: INT_RGB (%dx%d)", bImg.getWidth(), bImg.getHeight());
      int[] data = ((DataBufferInt) bImg.getRaster().getDataBuffer()).getData();
      ByteBuffer byteBuffer = ByteBuffer.allocate(data.length * 4);
      IntBuffer intBuffer = byteBuffer.asIntBuffer();
      intBuffer.put(data);
      aMat = new Mat(bImg.getHeight(), bImg.getWidth(), CvType.CV_8UC4);
      aMat.put(0, 0, byteBuffer.array());
      Mat oMatBGR = new Mat(bImg.getHeight(), bImg.getWidth(), CvType.CV_8UC3);
      Mat oMatA = new Mat(bImg.getHeight(), bImg.getWidth(), CvType.CV_8UC1);
      java.util.List<Mat> mixIn = new ArrayList<Mat>(Arrays.asList(new Mat[]{aMat}));
      java.util.List<Mat> mixOut = new ArrayList<Mat>(Arrays.asList(new Mat[]{oMatA, oMatBGR}));
      //A 0 - R 1 - G 2 - B 3 -> A 0 - B 1 - G 2 - R 3
      Core.mixChannels(mixIn, mixOut, new MatOfInt(0, 0, 1, 3, 2, 2, 3, 1));
      return oMatBGR;
    } else if (bImg.getType() == BufferedImage.TYPE_3BYTE_BGR) {
      log.error("makeMat: 3BYTE_BGR (%dx%d)", bImg.getWidth(), bImg.getHeight());
      byte[] data = ((DataBufferByte) bImg.getRaster().getDataBuffer()).getData();
      aMat = new Mat(bImg.getHeight(), bImg.getWidth(), CvType.CV_8UC3);
      aMat.put(0, 0, data);
      return aMat;
    } else if (bImg.getType() == BufferedImage.TYPE_4BYTE_ABGR) {
      log.trace("makeMat: TYPE_4BYTE_ABGR (%dx%d)", bImg.getWidth(), bImg.getHeight());
      byte[] data = ((DataBufferByte) bImg.getRaster().getDataBuffer()).getData();
      aMat = new Mat(bImg.getHeight(), bImg.getWidth(), CvType.CV_8UC4);
      aMat.put(0, 0, data);
      Mat oMatBGR = new Mat(bImg.getHeight(), bImg.getWidth(), CvType.CV_8UC3);
      Mat oMatA = new Mat(bImg.getHeight(), bImg.getWidth(), CvType.CV_8UC1);
      java.util.List<Mat> mixIn = new ArrayList<Mat>(Arrays.asList(new Mat[]{aMat}));
      java.util.List<Mat> mixOut = new ArrayList<Mat>(Arrays.asList(new Mat[]{oMatA, oMatBGR}));
      //A 0 - R 1 - G 2 - B 3 -> A 0 - B 1 - G 2 - R 3
      Core.mixChannels(mixIn, mixOut, new MatOfInt(0, 0, 1, 1, 2, 2, 3, 3));
      return oMatBGR;
    } else {
      log.error("makeMat: Type not supported: %d (%dx%d)",
              bImg.getType(), bImg.getWidth(), bImg.getHeight());
    }
    return aMat;
  }

  public static BufferedImage getBufferedImage(Mat mat) {
    return getBufferedImage(mat, dotPNG);
  }

  public static BufferedImage getBufferedImage(Mat mat, String type) {
    BufferedImage bImg = null;
    MatOfByte bytemat = new MatOfByte();
    if (SX.isNull(mat)) {
      mat = getNewMat();
    }
    Imgcodecs.imencode(type, mat, bytemat);
    byte[] bytes = bytemat.toArray();
    InputStream in = new ByteArrayInputStream(bytes);
    try {
      bImg = ImageIO.read(in);
    } catch (IOException ex) {
      log.error("getBufferedImage: %s error(%s)", mat, ex.getMessage());
    }
    return bImg;
  }

  //</editor-fold>

  //<editor-fold desc="***** capture">
  public Picture getAsPicture() {
    if (!hasContent()) {
      return new Picture();
    }
    return new Picture(this);
  }

  public Picture capture() {
    return getDevice().capture(this);
  }
  //</editor-fold>

  //<editor-fold desc="***** show, highlight">
  public void highlight() {
    highlight((int) SX.getOptionNumber("DefaultHighlightTime"));
  }

  public void highlight(int time) {
    //TODO Element.highlight not implemented
    log.error("highlight not implemented");
  }

  public static void fakeHighlight(boolean state) {
    //TODO implement fakeHighlight
  }

  private Color lineColor = Story.defaultlineColor;

  public Color getLineColor() {
    return lineColor;
  }

  public void setLineColor(Color lineColor) {
    this.lineColor = lineColor;
  }

  private int lineThickness = Story.defaultLineThickness;

  public int getLineThickness() {
    return lineThickness;
  }

  public void setLineThickness(int lineThickness) {
    this.lineThickness = lineThickness;
  }

  private int highLightLine = (int) SX.getOptionNumber("highLightLine", 1);

  public int getHighLightLine() {
    return highLightLine;
  }

  public void setHighLightLine(int highLightLine) {
    this.highLightLine = highLightLine;
  }

  private int showTime = Story.defaultShowTime;

//  public int getShowTime() {
//    return showTime;
//  }
//
//  public void setShowTime(int showTime) {
//    this.showTime = showTime;
//  }

  public void show() {
    show(showTime);
  }

  public void show(int time, int... times) {
    Story showing;
    showing = new Story(this, times);
    showing.show(time);
  }

  public Element showMatch(int... times) {
    if (hasMatch()) {
      Story showing = new Story(this);
      showing.add(getLastMatch());
      showing.show(times.length > 0 ? times[0] : showTime);
      return getLastMatch();
    }
    return null;
  }

  public void showVanish(int... times) {
    if (SX.isNotNull(getLastVanish())) {
      Story showing = new Story(this);
      showing.add(getLastVanish()).show(times.length > 0 ? times[0] : showTime);
    }
  }

  public List<Element> showMatches(int... times) {
    if (hasMatches()) {
      Story showing = new Story(this);
      for (Element match : getLastMatches()) {
        showing.add(match);
      }
      showing.show(times.length > 0 ? times[0] : showTime);
      return getLastMatches();
    }
    return null;
  }
  //</editor-fold>

  //<editor-fold desc="***** write, paste">
  public boolean write(String text) {
    //TODO implement write(String text)
    return true;
  }

  public boolean paste(String text) {
    //TODO implement paste(String text)
    return true;
  }
  //</editor-fold>

  //<editor-fold desc="***** keyboard">
  //TODO implement keyboard
  //</editor-fold>

  //<editor-fold desc="***** mouse">
  private Element findForClick(String type, Object... args) {
    Element target;
    if (args.length == 0) {
      target = this;
    } else if (args.length == 1) {
      target = Finder.runFind(type, args[0], this);
    } else {
      target = Finder.runWait(type, args[0], this, args[1]);
    }
    return target;
  }

  /**
   * Move the mouse to this element's target
   *
   * @return this
   */
  public Element hover(Object... args) {
    Element target = findForClick(Finder.HOVER, args);
    Element moveTarget = target.getDevice().move(target.getTarget());
    return moveTarget;
  }

  /**
   * Move the mouse to this element's target and click left
   *
   * @return this
   */
  public Element click(Object... args) {
    Element target = findForClick(Finder.CLICK, args);
    return target.getDevice().click(target);
  }

  /**
   * Move the mouse to this element's target and double click left
   *
   * @return this
   */
  public Element doubleClick(Object... args) {
    Element target = findForClick(Finder.DOUBLECLICK, args);
    target.getDevice().doubleClick(target);
    return target;
  }

  /**
   * Move the mouse to this element's target and click right
   *
   * @return this
   */
  public Element rightClick(Object... args) {
    Element target = findForClick(Finder.RIGHTCLICK, args);
    target.getDevice().rightClick(target);
    return target;
  }

  public Element dragDrop(Element from, Element to, Object... times) {
    Element targetFrom = null;
    if (SX.isNotNull(from)) {
      targetFrom = findForClick(Finder.DRAG, from);
    }
    Element targetTo = null;
    if (SX.isNotNull(to)) {
      targetTo = findForClick(Finder.DROP, to);
    }
    if (times.length == 0) {
      times = new Double[]{SX.getOptionNumber("Settings.MoveMouseDelay")};
    }
    targetTo = getDevice().dragDrop(targetFrom, targetTo, times);
    return targetTo;
  }

  public Element drag(Element from, Object... times) {
    return dragDrop(from, this, times);
  }

  public Element drag(Object... times) {
    return dragDrop(null, this, times);
  }

  public Element drop(Element to, Object... times) {
    return dragDrop(this, to, times);
  }

  public Element drop(Object... times) {
    return dragDrop(this, null, times);
  }
  //</editor-fold>

  //<editor-fold desc="***** waiting times">
  private double waitForThis = -1;

  public double getWaitForThis() {
    if (waitForThis < 0) {
      waitForThis = SX.getOptionNumber("Settings.AutoWaitTimeout");
    }
    return waitForThis;
  }

  public void setWaitForThis(double waitForThis) {
    this.waitForThis = waitForThis;
  }

  private double waitForMatch = -1;

  public double getWaitForMatch() {
    if (waitForMatch < 0) {
      waitForMatch = SX.getOptionNumber("Settings.AutoWaitTimeout");
    }
    return waitForMatch;
  }

  public void setWaitForMatch(double waitForMatch) {
    this.waitForMatch = waitForMatch;
  }

  private double lastWaitForThis = 0;

  public double getLastWaitForThis() {
    return lastWaitForThis;
  }

  public void setLastWaitForThis(double lastWaitForThis) {
    this.lastWaitForThis = lastWaitForThis;
  }

  private double lastWaitForMatch = 0;

  public double getLastWaitForMatch() {
    return lastWaitForMatch;
  }

  public void setLastWaitForMatch(double lastWaitForMatch) {
    this.lastWaitForMatch = lastWaitForMatch;
  }
  //</editor-fold>

  //<editor-fold desc="***** handle FindFailed, ImageMissing">
  public double getAutoWaitTimeout() {
    return autoWaitTimeout;
  }

  public void setAutoWaitTimeout(double autoWaitTimeout) {
    this.autoWaitTimeout = autoWaitTimeout;
  }

  double autoWaitTimeout = getWaitForMatch();

  private Event.RESPONSE findFailedResponse = Event.RESPONSE.ABORT;

  public Event.RESPONSE getFindFailedResponse() {
    return findFailedResponse;
  }

  public void setFindFailedResponse(Event.RESPONSE response) {
    findFailedResponse = response;
  }

  private Handler findFaileHandler = null;

  public void setFindFaileHandler(Handler handler) {
    findFaileHandler = handler;
  }

  public void unsetFindFaileHandler() {
    findFaileHandler = null;
  }

  private Event.RESPONSE imageMissingResponse = Event.RESPONSE.ABORT;

  public Event.RESPONSE getImageMissingResponse() {
    return imageMissingResponse;
  }

  public void setImageMissingResponse(Event.RESPONSE response) {
    imageMissingResponse = response;
  }

  private Handler imageMissingHandler = null;

  public void setImageMissingHandler(Handler handler) {
    imageMissingHandler = handler;
  }

  public void unsetImageMissingHandler() {
    imageMissingHandler = null;
  }
  //</editor-fold>

  //<editor-fold desc="***** observe">
  private Map<Element, Event> events = new HashMap<>();

  public long getObserveCount() {
    return observeCount;
  }

  private long observeCount = 0;

  private synchronized boolean setObserving(Integer state) {
    if (SX.isNotNull(state)) {
      if (state > 0) {
        observeCount++;
      } else if (state < 0) {
        observeCount--;
      } else {
        observeCount = 0;
      }
    }
    return observeCount > 0;
  }

  public boolean incrementObserveCount() {
    return setObserving(1);
  }

  public boolean decrementObserveCount() {
    return setObserving(-1);
  }

  public boolean isObserving() {
    return setObserving(null);
  }

  public void observe() {
    observeCount = 0;
    Events.add(this, events.values());
    Events.startObserving();
  }

  public void observeStop() {
    setObserving(0);
  }

  public void observeReset() {
    observeStop();
    events.clear();
    Events.remove(this);
  }

  private Event putEvent(Event.TYPE type, Object what, Handler handler) {
    if (what instanceof String) {
      Picture pWhat = new Picture((String) what);
      if (!pWhat.isValid()) {
        log.trace("handle image missing: %s", pWhat);
        if (!Picture.handleImageMissing(pWhat)) {
          log.error("Event: %s invalid what: %s", type, what);
          return null;
        }
      }
      what = pWhat;
    } else if (!(what instanceof Element)) {
      log.error("Event: invalid what: %s", what);
      return null;
    }
    Event evt = new Event(type, (Element) what, this, handler);
    if (events.containsKey(what)) {
      evt.setKey(events.get(what).getKey());
    } else {
      evt.setKey(events.size() + 1);
    }
    events.put((Element) what, evt);
    return evt;
  }

  public Event onAppear(Object what, Handler handler) {
    return putEvent(Event.TYPE.ONAPPEAR, what, handler);
  }

  public Event onAppear(Object what) {
    return putEvent(Event.TYPE.ONAPPEAR, what, null);
  }

  public Event onVanish(Object what, Handler handler) {
    return putEvent(Event.TYPE.ONVANISH, what, handler);
  }

  public Event onVanish(Object what) {
    return putEvent(Event.TYPE.ONVANISH, what, null);
  }

  private int minimumSizeDefault = 50;

  public Event onChange() {
    return putEvent(Event.TYPE.ONCHANGE, new Element(-minimumSizeDefault), null);
  }

  public Event onChange(Handler handler) {
    return putEvent(Event.TYPE.ONCHANGE, new Element(-minimumSizeDefault), handler);
  }

  public Event onChange(int minimumSize) {
    return putEvent(Event.TYPE.ONCHANGE, new Element(-minimumSize), null);
  }

  public Event onChange(int minimumSize, Handler handler) {
    return putEvent(Event.TYPE.ONCHANGE, new Element(-minimumSize), handler);
  }

  public void removeEvent(Event evt) {
    events.remove(evt);
  }

  public void removeEvents() {
    events.clear();
  }

  public boolean hasEvents() {
    return Events.hasHappened(this);
  }

  public Event nextEvent() {
    return Events.nextHappened(this);
  }
  //</editor-fold>

  //<editor-fold desc="***** find, ...">
  public Element find(Object... args) {
    return Do.find(target, this);
  }

  public Element wait(Object... args) {
    return Do.wait(target, this);
  }

  public boolean waitVanish(Object... args) {
    return Do.waitVanish(target, this);
  }

  public boolean exists(Object... args) {
    return Do.exists(target, this);
  }

  public List<Element> findAll(Object... args) {
    return Do.findAll(target, this);
  }
  //</editor-fold>

  //<editor-fold desc="***** Tool, Symbol">
  Element click = null;
  long clickedTime = 0;

  public boolean isClicked() {
    return SX.isNotNull(click);
  }

  public Element getClick() {
    return click;
  }

  public Element setClick(Element click) {
    this.click = click;
    click.setClicked(new Date().getTime());
    return this;
  }

  public void resetClick() {
    click = null;
  }

  public void setClicked(long clickedTime) {
    this.clickedTime = clickedTime;
  }

  public long getClicked() {
    return clickedTime;
  }

  public enum Component {
    RECTANGLE, CIRCLE, LINE, IMAGE, TEXT, BUTTON;
  }

  public Symbol setComponent(Component component) {
    this.component = component;
    return (Symbol) this;
  }

  public Component getComponent() {
    return component;
  }

  private Component component = Component.RECTANGLE;
  //</editor-fold>
}
