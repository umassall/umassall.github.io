import java.awt.*;
import java.applet.*;
import watch.*;/**/

/** This applet tests all possible events, printing those generated.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 24 July 96
  * @author Leemon Baird
  */
public class Events extends Applet {
  public void start() {
    TestWin t    =new TestWin(getGraphics());
    t.validate();
    t.layout();
    resize(500,500);
    t.resize(500,500);
  }
}

class TestWin extends Frame {
  int i=0;
  Graphics graphics;
  String[] ids=new String[20];
  List list    =new List(0,true);
  MenuBar mb   =new MenuBar();
  Menu    menu1=new Menu("test1");
  Menu    menu2=new Menu("test2");
  Choice  c    =new Choice();
  Button  b    =new Button("button");

  public TestWin(Graphics g) {
    graphics=g;
    c.addItem("choice1");
    c.addItem("choice2");
    menu1.add("test1a");
    menu1.add("test1b");
    menu2.add("test2a");
    menu2.add("test2b");
    mb.add(menu1);
    mb.add(menu2);
    list.addItem("first line");
    list.addItem("second line");
    list.addItem("third line");
    list.addItem("fourth line");

    setLayout(new FlowLayout());
    setMenuBar(mb);
    add(list);
    add(b);
    add(c);

    list.show();
    b.show();
    c.show();
    show();
  }
  public void resize(int x, int y) {
    super.resize(x,y);
    System.out.println("resize("+x+","+y+");");
  }
  public void resize(Dimension d) {
    super.resize(d);
    System.out.println("resize("+d+");");
  }
  public boolean handleEvent(Event e) {
    graphics.setColor(Color.white);
    graphics.fillRect(0,0,1000,1000);
    graphics.setColor(Color.black);
    i=(i+1)%ids.length;
    ids[i]=eventIdToString(e.id)+" "+e.id;
    for (int k=0,j=(i+2)%ids.length;j!=(i+1)%ids.length;j=(j+1)%ids.length,k++)
      if (ids[j]!=null)
        graphics.drawString(ids[j]+" key="+e.key,30,30+17*k);
    return super.handleEvent(e);
  }
  public boolean action(Event e,Object w) {
    System.out.println("(action: event=>"+e+", Object=>"+w+")");
    //if (e.target==c && "choice2".equals(""+w))
    //  c.select(0);
    //if (c.getSelectedIndex()==1)
    //  c.select(0);
    //if (c.getSelectedItem().equals("choice2"))
    //  c.select(0);
    return super.action(e,w);
  }

////////////////////////////////////////////////////////////
  String eventIdToString(int i) {
    switch (i) {
      case Event.ACTION_EVENT       : return "ACTION_EVENT"; //1001
      case Event.GOT_FOCUS          : return "GOT_FOCUS";    //1004 component now gets keys (must click in window, not just window bar at top)
      case Event.KEY_ACTION         : return "KEY_ACTION";   //403 pgup/pgdn/home/F1/etc pressed down
      case Event.KEY_ACTION_RELEASE : return "KEY_ACTION_RELEASE";
      case Event.KEY_PRESS          : return "KEY_PRESS";    //401 A/5/esc/ENTER/etc pressed down
      case Event.KEY_RELEASE        : return "KEY_RELEASE";  //402 key back up (either a 401 or 403)
      case Event.LIST_DESELECT      : return "LIST_DESELECT";
      case Event.LIST_SELECT        : return "LIST_SELECT";
      case Event.LOAD_FILE          : return "LOAD_FILE";    //1002
      case Event.LOST_FOCUS         : return "LOST_FOCUS";   //1005 no longer gets keys
      case Event.MOUSE_DOWN         : return "MOUSE_DOWN";   //501 mouse button pressed
      case Event.MOUSE_DRAG         : return "MOUSE_DRAG";   //506 mouse moved with button down
      case Event.MOUSE_ENTER        : return "MOUSE_ENTER";  //504 cursor now over window
      case Event.MOUSE_EXIT         : return "MOUSE_EXIT";   //505 cursor not over window anymore
      case Event.MOUSE_MOVE         : return "MOUSE_MOVE";   //503 mouse moved with no button pressed
      case Event.MOUSE_UP           : return "MOUSE_UP";     //502 mouse button released
      case Event.SAVE_FILE          : return "SAVE_FILE";    //1003
      case Event.SCROLL_ABSOLUTE    : return "SCROLL_ABSOLUTE";
      case Event.SCROLL_LINE_DOWN   : return "SCROLL_LINE_DOWN";
      case Event.SCROLL_LINE_UP     : return "SCROLL_LINE_UP";
      case Event.SCROLL_PAGE_DOWN   : return "SCROLL_PAGE_DOWN";
      case Event.SCROLL_PAGE_UP     : return "SCROLL_PAGE_UP";
      case Event.WINDOW_DEICONIFY   : return "WINDOW_DEICONIFY"; //doesn't work in Win95
      case Event.WINDOW_DESTROY     : return "WINDOW_DESTROY";   //201 click on close button
      case Event.WINDOW_EXPOSE      : return "WINDOW_EXPOSE";    //doesn't work in Win95
      case Event.WINDOW_ICONIFY     : return "WINDOW_ICONIFY";   //doesn't work in Win95
      case Event.WINDOW_MOVED       : return "WINDOW_MOVED";     //doesn't work in Win95
      default                       : return ""+i;
    } //end switch
  } //edn eventIdToString

  String keyToString(int i) {
    switch (i) {
      case Event.ALT_MASK           : return "ALT_MASK";
      case Event.CTRL_MASK          : return "CTRL_MASK";
      case Event.DOWN               : return "DOWN";         //1005
      case Event.END                : return "END";          //1001
      case Event.F1                 : return "F1";
      case Event.F2                 : return "F2";
      case Event.F3                 : return "F3";
      case Event.F4                 : return "F4";
      case Event.F5                 : return "F5";
      case Event.F6                 : return "F6";
      case Event.F7                 : return "F7";
      case Event.F8                 : return "F8";
      case Event.F9                 : return "F9";
      case Event.F10                : return "F10";
      case Event.F11                : return "F11";
      case Event.F12                : return "F12";
      case Event.HOME               : return "HOME";
      case Event.LEFT               : return "LEFT";
      case Event.META_MASK          : return "META_MASK";
      case Event.PGUP               : return "PGUP";         //1002
      case Event.PGDN               : return "PGDN";         //1003
      case Event.RIGHT              : return "RIGHT";
      case Event.SHIFT_MASK         : return "SHIFT_MASK";
      case Event.UP                 : return "UP";           //1004
      default                       : return ""+(char)i;
    } //end switch
  }// end keyToString
}//end class Test
