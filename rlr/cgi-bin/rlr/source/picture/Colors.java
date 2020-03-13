package picture;
import java.awt.*;

/** A general color passing through a PicPipe pipeline, (has 6 components).
  * Typically, either the double value is used, or the 5 bytes are used,
  * but not both.  In the later stages, the filter and transparency are
  * ignored and only red/green/blue are relevant.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 26 April 96
  * @author Leemon Baird
  */
public class Colors {
  /** use double value instead of colors in early PicPipe stages */
  public double value =0;
  /** red component of color, -128=black, 127=bright red */
  public byte  red   =-128;
  /** green component of color, -128=black, 127=bright red */
  public byte  green =-128;
  /** blue component of color, -128=black, 127=bright red */
  public byte  blue  =-128;
  /** add in color like a sheer curtain, -128=invisible curtain, 127=opaque curtain */
  public byte  filter=-128;
  /** multiply color like stained glass, -128=invisible glass, 127=can block all or none */
  public byte  trans =-128;

  /** Set the color to black (don't change value, filter, or transparency) */
  public final void setBlack() {red=green=blue=(byte)-128;}
  /** Set the color to white (don't change value, filter, or transparency) */
  public final void setWhite() {red=green=blue=(byte)127;}
  /** Set the color to gray (don't change value, filter, or transparency) */
  public final void setGray() {red=green=blue=(byte)0;}
  /** Set the color to red (don't change value, filter, or transparency) */
  public final void setRed() {red=(byte)127;green=blue=(byte)-128;}
  /** Set the color to green (don't change value, filter, or transparency) */
  public final void setGreen() {green=(byte)127;red=blue=(byte)-128;}
  /** Set the color to blue (don't change value, filter, or transparency) */
  public final void setBlue() {red=green=(byte)-128;blue=(byte)127;}
  /** Set the color to yellow (don't change value, filter, or transparency) */
  public final void setYellow() {red=green=(byte)127;blue=(byte)-128;}
  /** Set the color to aqua (don't change value, filter, or transparency) */
  public final void setAqua() {red=(byte)-128;green=blue=(byte)127;}
  /** Set the color to purple (don't change value, filter, or transparency) */
  public final void setPurple() {red=blue=(byte)127;green=(byte)-128;}

  /** Reset the 6 Colors components to all -128, and value to 0. */
  public final void reset() {
    red=green=blue=filter=trans=-128;
    value=0;
  }
  /** Set colors given 3 bytes in the range [-128,127] */
  public final void setRGB(byte r, byte g, byte b) {
    red  =r;
    green=g;
    blue =b;
  }
  /** Set colors given 3 doubles in the range [0,1] */
  public final void setRGB(double r, double g, double b) {
    red  =(byte)(r*255-128);
    green=(byte)(g*255-128);
    blue =(byte)(b*255-128);
  }
  /** Set colors given 3 doubles (hue, saturation, brightness) in the range [0,1] */
  public final void setHSB(float h, float s, float b) {
    int rgb=Color.HSBtoRGB(h,s,b);

    red  =(byte)(((rgb>>16)& 0xff)*255-128);
    green=(byte)(((rgb>>8 )& 0xff)*255-128);
    blue =(byte)(((rgb    )& 0xff)*255-128);
  }
  /** Given an array of 3 doubles, changes them to be hue, saturation brightness in range [0,1]. */
  public final void getHSB(float hsb[]) {
    Color.RGBtoHSB(red,green,blue,hsb);
  }
  /** Set colors given 5 bytes in the range [-128,127] */
  public final void set(byte r, byte g, byte b, byte f, byte t) {
    red   =r;
    green =g;
    blue  =b;
    filter=f;
    trans =t;
  }
  /** Set colors given 5 doubles in the range [0,1] */
  public final void set(double r, double g, double b, double f, double t) {
    red   =(byte)(r*255-128);
    green =(byte)(g*255-128);
    blue  =(byte)(b*255-128);
    filter=(byte)(f*255-128);
    trans =(byte)(t*255-128);
  }
  /** Copy one Colors object into the fields of another so they're the same. */
  public final void copyInto(Colors color) {
    color.red   =red;
    color.green =green;
    color.blue  =blue;
    color.filter=filter;
    color.trans =trans;
  }
} //end class Colors
