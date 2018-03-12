import Display;
import watch.*;
import pointer.*;
import java.awt.*;
import java.util.Vector;
import parse.*;
import expression.*;
import matrix.*;

/** This Display shows the names of variables and their values, and
  * allows the user to edit them.  Any variable that has been made
  * watchable can be seen and changed by the user this way.
  *    <p>This code is (c) 1996,1997 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.01, 13 Feb 97
  * @author Leemon Baird
  */
public class ShowEdit extends Display {
  private Parsable[][] varNames={new PString[0],null}; //list of variable names to show and edit
  private String[]     names={};                       //the names of the variables with extra space added
  private Pointer[]    vars=new Pointer[names.length]; //pointers to variables associated with each name
  private int          labelWidth=0;                   //width of the longest label
  private int          numWidth=0;                     //width of the longest number
  private Pointer      triggerVar=null;                //pointer to the trigger variable (returned from watchManager.registerWatch)
  private IntExp       frequency=new IntExp(-1);       //plot every frequency changes in trigger variable (never if frequency=null)
  private PString      triggerName=new PString(null);  //name of the variable that controls when updating happens (e.g. timestep number)
  private List         list;                           //scrollable list of name/value pairs
  private int          indent;                         //values are indented this many pixels
  private FontMetrics  fontMetrics;                    //used to figure out width of strings
  private TextField    newVal;                         //user edits the new value here
  private int          newInd=-1;                      //index of value being changed (-1=nothing selected now)
  private int          oldInd=0;                       //index of last value changed (=newInd if newInd>-1)
  private String       origVal;                        //value before user edited it
  private PString      allPrefix=new PString(null);    //show all names with this prefix

  private Object[][] parameters=
    {{"Show variables. "+
      "Shows either all variables with a given prefix, "+
      "or just the named variables.  The user can edit whatever "+
      "is shown.  If a trigger is given, the window will update itself every "+
      "time the    given variable changes the given number of times."},
     {"trigger",   triggerName,"update all the values every freq times that this variable changes",
      "freq",      frequency,  "updates every freq times (-1=never)",
      "all",       allPrefix,  "if not null, will show/edit all variable names starting with this string",
      "vars",      varNames,   "list of variables to show and edit in the window"},
     {}};

  /** Return a parameter array if BNF(), parse(), and unparse() are to be automated, null otherwise.
    * @see parse.Parsable#getParameters
    */
  public Object[][] getParameters(int lang) {
    return parameters;
  }

  /** constructor that does nothing */
  public ShowEdit() {super();} //CAFE bug: this must be here, even though it's supposedly redundant

  /** This constructor takes the same parameters that parse() parses, and
    * initializes this object just as if it had been created by parsing instead.
    * If showAll then it shows all variables (or all starting with prefix, if
    * prefix is not null), else it shows those variables whose
    * names are in varNames[].  It updates every freq times that trigger changes.
    */
  public ShowEdit(String trigger, IntExp freq,
                  boolean showAll, String prefix, String[] vars) {
    setBackground(Color.white);
    triggerName.val = trigger;
    frequency.val   = freq.val;
    allPrefix.val   = prefix;
    if (vars!=null) {
      varNames[0]= new PString[vars.length];
      for (int i=0;i<vars.length;i++)
        varNames[0][i]=new PString(vars[i]);
    }
  }

  /** if a list element is selected, change the last value
    * editted, and start editing the value of the new
    * item selected. Allows editting of any primitive
    * type (double,float,int,long,short,string,char,boolean).
    */
  public boolean handleEvent(Event evt) {
    String s; //what the user changed the value to
    if (newInd==-1 &&
        (evt.id==Event.KEY_PRESS ||
         evt.id==Event.KEY_ACTION)) { //key hit when nothing is selected
      ///**/int rowsPerPage=list.getRows(); //CAFE bug: this returns total # of rows, rather than rows per page
      int rowsPerPage=list.countItems() *  //this hack still doesn't fix the bug perfectly. Sometimes it doesn't scroll far enough
                      list.bounds().height /
                      list.minimumSize(list.countItems()).height;
      if      (evt.key==Event.PGUP) oldInd-=rowsPerPage;
      else if (evt.key==Event.PGDN) oldInd+=rowsPerPage;
      else if (evt.key==Event.UP)   oldInd--;
      else if (evt.key==Event.DOWN) oldInd++;
      oldInd= oldInd<0 ? 0 : oldInd>list.countItems()-1 ? list.countItems()-1 : oldInd; //clip to legal range
      list.select(oldInd);
      newInd=oldInd;
      origVal=(newInd==-1) ? "" : ""+vars[newInd];
      newVal.setText(origVal);
      newVal.selectAll();
      newVal.requestFocus();
      if (evt.key!=Event.UP   &&  //if the key hit was a character to put into the field
          evt.key!=Event.DOWN &&
          evt.key!=Event.PGUP &&
          evt.key!=Event.PGDN &&
          evt.key!=' '        &&
          evt.key!='\n'       &&
          evt.key!='\r') {
        newVal.setText(""+(char)(short)evt.key);
        newVal.select(1,1); //put the cursor after this character
      }
      return true; //no need for any events to propagate to parents or containers
    }

    if ((evt.id==Event.LIST_SELECT) ||
        ((evt.id==Event.KEY_PRESS || evt.id==Event.KEY_ACTION)  &&
         (evt.key=='\n'       || evt.key=='\r'       ||
          evt.key==Event.UP   || evt.key==Event.DOWN ||
          evt.key==Event.PGUP || evt.key==Event.PGDN))) { //hit ENTER or clicked elsewhere
      s=newVal.getText();
      if (newInd>-1) { //if something was selected before this event,
        if (!s.equals(origVal) && !s.equals("")) { //if user changed it, then save changes
          try {
            if (vars[newInd] instanceof PDouble)
              ((PDouble)vars[newInd]).val=Double.valueOf(s).doubleValue();
            else if (vars[newInd] instanceof PInt)
              ((PInt)vars[newInd]).val=Integer.valueOf(s).intValue();
            else if (vars[newInd] instanceof PLong)
              ((PLong)vars[newInd]).val=Long.valueOf(s).longValue();
            else if (vars[newInd] instanceof PShort)
              ((PShort)vars[newInd]).val=(short)Integer.valueOf(s).intValue();
            else if (vars[newInd] instanceof PFloat)
              ((PFloat)vars[newInd]).val=Float.valueOf(s).floatValue();
            else if (vars[newInd] instanceof PString)
              ((PString)vars[newInd]).val=s;
            else if (vars[newInd] instanceof PChar)
              ((PChar)vars[newInd]).val=s.charAt(0);
            else if (vars[newInd] instanceof PBoolean) {
              s=s.trim();
              ((PBoolean)vars[newInd]).val=
                  !(s.equalsIgnoreCase("false") ||
                    s.equalsIgnoreCase("no") ||
                    s.equalsIgnoreCase("f") ||
                    s.equalsIgnoreCase("n") ||
                    s.equalsIgnoreCase("0") ||
                    s.equalsIgnoreCase("nil") ||
                    s.equalsIgnoreCase("()") ||
                    s.equalsIgnoreCase(""));
            } else if (vars[newInd] instanceof PMatrixD) {
              Parser pp=new Parser(s);
              try {
                MatrixD matrix=(MatrixD)pp.parseClass("MatrixD",0,true);
                ((PMatrixD)vars[newInd]).val.replace(matrix);
              } catch (MatrixException e) {//if a bad input, just ignore it
              }
            }
            list.replaceItem(names[newInd]+vars[newInd],newInd);
            watchManager.update(); //let watchManager know vars[newInd] just changed
          } catch (NumberFormatException e) {
          } catch (Throwable e) { //if anything goes wrong, just forget it.
          }
        }//end if user changed it
        if (evt.key==Event.UP   || evt.key==Event.DOWN ||
            evt.key==Event.PGUP || evt.key==Event.PGDN) {
          newInd=-1; //now nothing is selected
          handleEvent(evt); //having deselected this item, now do it again to select the new item
          return true;
        }
      }//end if something was selected before this event
      if (evt.id==Event.LIST_SELECT) { //show new item for editting
        newInd=((Integer)evt.arg).intValue();
        oldInd=newInd;
        origVal=(newInd==-1) ? "" : ""+vars[newInd];
        newVal.setText(origVal);
        newVal.selectAll();
        newVal.requestFocus();
      } else { //if hit ENTER, nothing selected, nothing to edit
        if (newInd>-1)
          list.deselect(newInd);
        newInd=-1;
        origVal="";
        newVal.setText(origVal);
        requestFocus(); //just so the textField no longer has the focus
      }
      return true; //don't let parent see the list select or ENTER
    }//end if KEYPRESS(ENTER) or LIST_SELECT
    return false; //let keystrokes propagate to text field
  }//end handleEvent

  /** return the width this window should be */
  int bestWinWidth() {
    return indent+100;
  }

  /** return the height this window should be */
  int bestWinHeight() {
    return fHeight()*(names.length+2)+25;
  }

  /** One of the watched variables has been unregistered.
    */
  public void unregister(String watchedVar) {
    String[] newNames=new String[names.length-1];
    int i=0;
    for (int j=0;j<names.length;j++)
      if (i<newNames.length && !watchedVar.equals(names[j].trim()))
        newNames[i++]=names[j];
    if (i==newNames.length)
      names=newNames;
    update(null,null,null); //redraw the new screen
  } //end unregister

  /** One of the watched variables has changed, so look at it and others.
    */
  public void update(String changedName, Pointer changedVar, Watchable obj) {
    if (!disableDisplays)
      try { //When window closes, I may still be here briefly after peer is gone
        newInd=list.getSelectedIndex();
        list.disable(); //mouse clicks while hidden crash JDK 1.0, so disable them
        list.hide();
        if (names!=null && list!=null)
          for (int i=0;i<names.length;i++) {
            list.replaceItem(names[i]+vars[i],i);
          }
        list.show();
        list.enable();
        if (newInd>-1)
          list.select(newInd);
      } catch (Throwable e) {
      }
    super.update(changedName,changedVar,obj); //repaints screen if appropriate
  } //end update

  /** Initialize, either partially or completely.
    *@see parse.Parsable#initialize
    */
  public void initialize(int level) {
    triggerVar=watchManager.registerWatch(triggerName.val,frequency,this);

    String[] prefixedNames=new String[0];  //names of vars starting with prefix, if any
    if (allPrefix!=null && allPrefix.val!=null)
      prefixedNames=watchManager.getAllVars(allPrefix.val);
    names=new String [prefixedNames.length+varNames[0].length]; //names[] will include all variables listed by user, plus all starting with given prefix
    vars =new Pointer[names.length];
    int j;
    for (j=0;j<varNames[0].length;j++) {            //put the list of given vars into names[]
      names[j]=((PString)(varNames[0][j])).val;
      vars[j]=watchManager.findVar(names[j]);
    }
    for (int i=0;i<prefixedNames.length;i++,j++) { //put the vars with given prefix into names[]
      names[j]=prefixedNames[i];
      vars[j]=watchManager.findVar(allPrefix+names[i]);
    }


    list=new List(names.length,false);
    list.setBackground(Color.white);
    for (int i=0;i<names.length;i++)
      list.addItem("");

    indent=0;
    fontMetrics=getFontMetrics(getFont());
    for (int i=0;i<names.length;i++) { //find widest name for indenting
      int width=fontMetrics.stringWidth(names[i]);
      if (width>indent)
        indent=width;
    }
    for (int i=0;i<names.length;i++)
      for (;fontMetrics.stringWidth(names[i])<indent+5;)
        names[i]+=" "; //add enough spaces so value is indented correctly
    setLayout(new BorderLayout(0,0));
    add("Center",list);
    newVal=new TextField((newInd==-1) ? "" : ""+vars[newInd]);
    add("South",newVal);
    list.show();
    layout();
    update(null,null,null); //redraw the new screen
  }//end initialize
} //end ShowEdit
