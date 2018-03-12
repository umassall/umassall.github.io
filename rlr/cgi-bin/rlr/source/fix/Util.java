package fix;
import Display;
import java.awt.*;

/** This class contains various utilities for overcomming
  * bugs or design shortcomings in the Java implementations that
  * I have available right now.
  *
  * It is recommended that the package fix.* never be imported, so that
  * there is no confusion between java.* classes and fix.* classes of
  * the same name.
  *
  *    <p>This code is (c) 1997 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.1 29 Mar 97
  * @author Leemon Baird
  */

public final class Util {
  // v. 1.1 29 Mar 97 added toString(), improved paintAll() on some AWT components
  // v. 1.0 19 Feb 97

  /** logically, the call:
    *      myComponent.paintAll(graphicsContext)
    * should paint the Component myComponent, and if it is
    * a container, should paint its contents.  If g is an
    * arbitrary Graphics (e.g. an offscreen buffer), then
    * Component.paintAll() doesn't work.
    * The following at least works for user-defined Components
    * where paint() has been overridden.  It doesn't work for
    * all the standard AWT components with the original paint().
    * The call given above should be translated into:
    *      fix.Util.paintAll(graphicsContext,myComponent);
    * Normally, changeOriginal should be false, so the translating
    * and clipping won't damage the original Graphics.  But if the
    * Graphics is something unusual, such as a front end to
    * Postscript output to a file, then it may be preferable to
    * change the original and do all the drawing there rather than
    * drawing to a copy of the Graphics and then dumping a bit map
    * of the result back into the original.  The parameter flicker
    * is true if double buffering should be turned off in the
    * Display objects, else it will be turned on in them.
    */
  public static void paintAll(Graphics g,Component c,boolean changeOriginal,boolean flicker) {
    Rectangle b=c.bounds();
    int h2 = b.height/2;     //half of the height of the window

    Graphics gg=changeOriginal ? g : g.create();
    gg.clipRect(b.x,b.y,b.width,b.height);
    gg.translate(b.x,b.y);

    boolean origFlicker=false;
    if (c instanceof Display) {             //control whether Display objects double buffer
      origFlicker=((Display)c).flicker.val;
      ((Display)c).flicker.val=flicker;
    }
    c.update(gg); //let a single component paint itself
    if (c instanceof Display)               //don't affect whether the Display double buffers on the screen
      ((Display)c).flicker.val=origFlicker;

    //The following helps some components that can't paint themselves
    if (c instanceof Button) {
      if (c.getFont() != null)
        gg.setFont(c.getFont());
      gg.setColor(Color.white);
      gg.fillRoundRect(0,0,b.width, b.height, 4,4);
      gg.setColor(Color.black);
      gg.drawRoundRect(0,0,b.width, b.height, 4,4);
      gg.drawString(((Button) c).getLabel(), 2, h2+3);
    } else if (c instanceof java.awt.Label) {
      java.awt.Label label=(java.awt.Label) c;
      java.awt.FontMetrics fm=label.getFontMetrics(label.getFont());
      String t=label.getText();
      int a=label.getAlignment();
      if (c.getFont() != null)
        gg.setFont(c.getFont());
      gg.setColor(java.awt.Color.black);
      gg.setPaintMode();
      gg.drawString(t,(a==java.awt.Label.CENTER) ? (b.width-fm.stringWidth(t))/2
                    : (a==java.awt.Label.RIGHT)  ?  b.width-fm.stringWidth(t)
                    : 0, h2+fm.getHeight()/3);
    } else if (c instanceof Choice) {
      if (c.getFont() != null)
        gg.setFont(c.getFont());
      gg.setColor(Color.black);
      gg.drawRect(0,0,b.width+1, b.height+1);
      gg.setColor(Color.white);
      gg.fillRect(0,0,b.width, b.height);
      gg.setColor(Color.black);
      gg.drawRect(0,0,b.width, b.height);
      gg.fillRect(b.width-7,h2-1, 6,2);
      gg.drawString(((Choice) c).getSelectedItem(), 2, h2+3);
    } else if (c instanceof TextComponent) {
      if (c.getFont() != null)
        gg.setFont(c.getFont());
      gg.setColor(Color.white);
      gg.fillRect(0,0,b.width, b.height);
      gg.setColor(Color.black);
      gg.drawRect(0,0,b.width, b.height);
      gg.drawString(((TextComponent) c).getText().trim(), 2, h2+3);
    } else if (c instanceof Container) {
      int n=((Container)c).countComponents();
      for (int i=n-1;i>=0;i--)
        paintAll(gg,((Container)c).getComponent(i),changeOriginal,flicker);  //recurse on others

    if (gg!=g)
      gg.dispose();
    }
  }

  /** Create a new instance of this class.
    * According to the Java 1.0 specs, you should be able to say:
    *        MyClass x=new("MyClass");
    * But the JDK-1.0 compiler calls it a feature that is not yet implemented,
    * so instead, say:
    *        MyClass x=fix.Util.new_("MyClass");
    */
  public static final Object new_(String s) {
    try {
      return  (Class.forName(s)).newInstance(); //load and instantiate the class
    } catch (java.lang.ClassNotFoundException e) {
    } catch (java.lang.InstantiationException e) {
    } catch (java.lang.IllegalAccessException e) {
    } catch (java.lang.ClassCastException     e) {
    } catch (java.lang.Throwable    e) {//this shouldn't happen
      System.out.println("error '"+e.getMessage()+
                         "' in fix.Util.new_() while creating class "+s);
      e.printStackTrace();
    }
    return null; //if it doesn't exist, return a null pointer
  } //end new

  /** Convert a float or double to a string, rounding to display at
    * most dig digits, and converting to scientific notation
    * if necessary to ensure that a minimum of msf significant figures appear
    * in numbers like 0.000123.
    * This is particularly useful for running
    * Java 1.0 programs (which have no way to control the precision
    * when printing numbers) on a Java 1.1 VM (which tends to use
    * lots of decimal places by default).
    */
  public static String toString(double x, int dig, int msf) {
    char[] chars=new char[dig+11]; //used in mantissa() to hold fractional part
    int [] charsIndex=new int[1];  //1-element array holds # characters already put into chars[]
    charsIndex[0]=0; //assigned this way rather than charsIndex={0} so this method is reentrant

    if (-x==0                ||
         x==0                ||
        Double.isInfinite(x) ||
        Double.isNaN(x))
      return ""+x;

    if (x<0) {
      chars[charsIndex[0]++]='-';
      x=-x;
    }

    long exp;
    exp=Math.round(Math.floor(Math.log(x)/Math.log(10)));
    x+=5*Math.pow(10,exp-dig);   //round to that many digits
    exp=Math.round(Math.floor(Math.log(x)/Math.log(10))); //did that add a digit?

    if (exp>=msf-dig && exp<dig) {//don't use scientific notation if not needed
      if (exp>-1) //when it's zero point something, count the zero as a digit too
        dig-=exp;
      toChars(chars,charsIndex,dig-1,x);
    } else { //use scientific notation
      double pow=Math.pow(10.,exp);
      toChars(chars,charsIndex,dig-1,x/pow);
      chars[charsIndex[0]++]='E';
      toChars(chars,charsIndex,0,exp);
    }

    return new String(chars,0,charsIndex[0]);
  }//end toString


  //convert a nonegative double to characters (not scientific notation), and
  //put them into the array starting at index charsIndex[0].
  //Truncates to dp decimal places, and removes trailing zeros and decimal points.
  private static void toChars(char[] chars, int[] charsIndex, int dp, double num) {
    double x=num;
    if (x<0) {
      chars[charsIndex[0]++]='-';
      x=-x;
    }
    x=x/10.; //ensure at least one digit left of the decimal place (e.g. 0.5 rather than .5)
    int d;   //current digit
    int dig; //number of digits to left of decimal point
    for (dig=dp+1;x>=1.;dig++)  //count # digits to output, and get x < 1,0
      x=x/10.;
    for (;dig>0;dig--) { //output each digit
      if (dig==dp)
        chars[charsIndex[0]++]='.';  //add in the decimal point at the right spot
      x*=10.;
      d=(int)Math.round(Math.floor(x));
      x-=d;
      chars[charsIndex[0]++]=Character.forDigit(d,10);
    }
    while (dp>0 && chars[charsIndex[0]-1]=='0')  //strip trailing zeros after decimal point
      charsIndex[0]--;
    if (chars[charsIndex[0]-1]=='.') //strip a trailing decimal point
      charsIndex[0]--;
  } //end toChars
}//end Util
