import java.awt.*;

/** Draw the WebSim logo at any location with any size
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 22 Oct 96
  * @author Leemon Baird
  */
public class Logo {
  /** Draw the logo and credits at location (x,y).
    * This fits within a rectangle 250 pixels wide by 44 high
    * on Win95 with the default font.
    */
  public static final void credits(Graphics g,int x,int y) {
    g.drawString("WebSim (c)1996,1997",     x+70,y+15);
    g.drawString("Freeware",                x+70+25,y+30);
    g.drawString("(See source for details)",x+70,y+45);
    Logo.draw(g,x,y,48,Color.blue,Color.green,5);
  }

  /** Draw the logo with upper-left corner at (x,y) and given width.
    * The width is rounded down to the nearest multiple of 16 pixels.
    * If the width is 16*n, then the height will be 13*n
    */
  public static final void draw(Graphics g, int x, int y, int width) {
    draw(g,x,y,width/16,width/32,width/8,Color.black,Color.white,0);
  }

  /** Draw the logo with upper-left corner at (x,y) and given width.
    * The width is rounded down to the nearest multiple of 16 pixels.
    * If the width is 16*n, then the height will be 13*n.  The two
    * colors define the foreground and background color.
    */
  public static final void draw(Graphics g, int x, int y, int width,
                                Color fore, Color back, int border) {
    draw(g,x,y,width/16,width/32,width/8,fore,back,border);
  }

  /** Draw the logo with upper-left corner at (x,y) and shaped according
    * to the given parameters (THIS METHOD IS NOT RECOMMENDED).
    * It is better to use draw(g,x,y,width) or draw(g,x,y,fore,back) to
    * ensure that the logo has the standard proportions. Fore and Back
    * are the colors for the foreground and background.  If Back is null,
    * then the background is transparent.
    */
  public static final void draw(Graphics g, int x, int y,
                                int b, int c, int w,
                                Color fore, Color back,int f) {
    //b is the width of the bar
    //c is the width of the crack at an intersection
    //w is the width of the whitespace (including 2 cracks)
    //f is the frame, the amount of background on each side of logo

    if (back!=null) {
      g.setColor(back);
      g.fillRect(x,y,6*b+5*w+2*f,5*b+4*w+2*f);
    }
    g.setColor(fore);

    g.fillRect(f+x,           f+y,         3*b+2*w,   b); //horizontal 1 left
    g.fillRect(f+x+3*b+3*w,   f+y,         3*b+2*w,   b); //horizontal 1 right
    g.fillRect(f+x+b+w,       f+y+b+w,     2*b+2*w-c, b); //horizontal 2 left
    g.fillRect(f+x+4*b+3*w+c, f+y+b+w,     b+w-c,     b); //horizontal 2 right
    g.fillRect(f+x,           f+y+2*b+2*w, 2*b+2*w-c, b); //horizontal 3 left
    g.fillRect(f+x+3*b+2*w+c, f+y+2*b+2*w, b+2*w-2*c, b); //horizontal 3 middle
    g.fillRect(f+x+5*b+4*w+c, f+y+2*b+2*w, b+w-c,     b); //horizontal 3 right
    g.fillRect(f+x+b+w,       f+y+3*b+3*w, 3*b+2*w,   b); //horizontal 4
    g.fillRect(f+x+2*b+2*w,   f+y+4*b+4*w, 3*b+2*w,   b); //horizontal 5

    g.fillRect(f+x,           f+y,           b, 3*b+2*w  ); //vertical 1
    g.fillRect(f+x+b+w,       f+y+b+w,       b, b+w-c    ); //vertical 2 top
    g.fillRect(f+x+b+w,       f+y+3*b+2*w+c, b, b+w-c    ); //vertical 2 bottom
    g.fillRect(f+x+2*b+2*w,   f+y,           b, b+w-c    ); //vertical 3 top
    g.fillRect(f+x+2*b+2*w,   f+y+2*b+w+c,   b, b+2*w-2*c); //vertical 3 middle
    g.fillRect(f+x+2*b+2*w,   f+y+4*b+3*w+c, b, b+w-c    ); //vertical 3 bottom
    g.fillRect(f+x+3*b+3*w,   f+y,           b, 2*b+2*w-c); //vertical 4 top
    g.fillRect(f+x+3*b+3*w,   f+y+3*b+2*w+c, b, b+w-c    ); //vertical 4 bottom
    g.fillRect(f+x+4*b+4*w,   f+y+b+w,       b, 4*b+3*w  ); //vertical 5
    g.fillRect(f+x+5*b+5*w,   f+y,           b, 3*b+2*w  ); //vertical 6
  }
} // end class Logo
