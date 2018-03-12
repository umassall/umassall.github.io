package watch;
import java.util.Vector;
import pointer.*;
import parse.*;
import watch.*;

/** This object manages the relationship between a Watchable object
  * and the various Watchers that watch it.  The Watchable object
  * registers several of its input variables, and the WatchManager
  * informs each Watcher when a variable it is interested in changes,
  * or every nth time it changes.  The WatchManager should be in the
  * same thread as the Watchable object, but each Watcher may be
  * in a separater thread, with an update() method.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 3 June 96
  * @author Leemon Baird
  */
public class WatchManager {
  /**/ //when a variable is first registered, it should remember its value,
       //so it won't automatically call all the associated update() routines
       //on the watchers the first time the watchManager's update() is called.

  /** The name of each registered variable */
  protected Vector regName = new Vector();
  /** The Watchable object containing each registered variable */
  protected Vector regWatchable = new Vector();
  /** The pointer to each registered variable */
  protected Vector regPtr = new Vector();

  /** The name of each watched variable */
  protected String[] name={};
  /** The Watcher for each watched variable */
  protected Watcher[] watcher={};
  /** The Watchable object containing each watched variable */
  protected Watchable[] watched={};
  /** The variable to be watched for each watched variable */
  protected Pointer[] var={};
  /** The previous value of each watched variable */
  protected Pointer[] prevVar={};
  /** The frequency of each watched variable */
  protected PInt[] freq={};
  /** The number of times each watched variable has changed since its Watcher was last called */
  protected int[] changes={};

  /** The time (in milliseconds) when this class first started. */
  protected long startTime=System.currentTimeMillis();
  /** The total number of milliseconds spent on all updates so far */
  protected long updateTime=0;
  /** The percentage of time spent in updates so far */
  protected PDouble updatePercent=new PDouble(0);
  /** The profiling is done for periods of this many seconds */
  protected PDouble profilePeriod=new PDouble(30);
  /** sleep for sleepTime milliseconds and yield every sleepFreq calls to update() */
  protected PInt sleepFreq=null;
  /** sleep for sleepTime milliseconds and yield every sleepFreq calls to update() */
  protected PInt sleepTime=null;
  /** number of calls to update() since last yield and sleep */
  protected int numUpdates=0;

  /** default constructor sleeps 20 milliseconds every 1000 calls to update().*/
  public WatchManager() {
    int freq=1000, time=20;
    initializeEverything(freq,time);
  }

  /** watchManager will yield and sleep(time) every freq calls to update().
    * If freq=-1, then it never yields or sleeps.  If time=-1, then it
    * never sleeps.
    */
  public WatchManager(int freq, int time) {
    initializeEverything(freq,time);
  }

  private static PInt f=new PInt(1); //frequency for registered watches

  /** register all the parameters for a Watcher object as both
    * watchable variables and as watches.  This lets other
    * objects see those variables, the user can change them at
    * runtime, and if any of them change, the object's update()
    * method is automatically called so it can respond.  The object
    * passed in must be both Parsable and a Watcher or this will
    * do nothing.
    */
  public void registerParameters(Parsable obj,String prefix) {
    Parsable   p1;
    Parsable[] p2;
    Object[][] par=obj.getParameters(1); //assuming language 1 here /**/
    if (par==null)  //if this object does BNF/parse/unparse manually, then don't register anything
     return;
///**/    if (!(obj instanceof Watcher))
///**/      System.out.println("Error: WatchManager.registerParameters() called with obj "+obj+"not a Watcher");

    for (int i=0;i<par[1].length;i+=3) {
      try {
        p1=(Parsable)par[1][i+1];
        registerVar  (prefix+par[1][i],(Pointer)par[1][i+1],(Watchable)obj);
        registerWatch(prefix+par[1][i],f,(Watcher)obj);
      } catch (ClassCastException e) {
      } //no need to register a watch for something that isn't a Pointer
    }
  }//end parseObject

  //register profiling and timing controls and register self with static array all[]
  private void initializeEverything(int freq, int time) {
    sleepFreq=new PInt(freq);
    sleepTime=new PInt(time);
    registerVar("update time %",     updatePercent,null);
    registerVar("profile period",    profilePeriod,null);
    registerVar("thread yield freq", sleepFreq,    null);
    registerVar("thread sleep time", sleepTime,    null);
  } //end initializeEverything

  /** Return all watchable variables with prefix allPrefix.
    * If variable foo is in group "/a/b/c/" then the list
    * will include "/a/b/c/foo".
    */
  public synchronized String[] getAllVars(String allPrefix) {
    String[] vars;
    Vector v=new Vector();
    for (int i=0;i<regName.size();i++)
      if (((String)regName.elementAt(i)).startsWith(allPrefix))
        v.addElement(((String)regName.elementAt(i)).substring(allPrefix.length()));
    vars=new String[v.size()];
    v.copyInto(vars);
    return vars;
  }

  /** Given the group, return a list of all its watchable variables.
    * If variable foo is in group /a/b/c/, then getVars("/a/b/c/")
    * returns a list including "/a/b/c/foo".  If /a/b/c/d/ is also
    * a group, then the list will include "/a/b/c/d/" but will
    * not include any other string starting with "/a/b/c/d/".
    */
  public synchronized String[] getVars(String group) {
    String[] v;               //the strings to return
    Vector vars=new Vector(); //the strings to return
    String name;              //name of one variable on the list
    int pos;                  //position of first / after group
    int k;                    //index for searching through previous variables

    for (int i=0;i<regName.size();i++) { //copy matches into a new vector
      name=(String)regName.elementAt(i);
      if (name.startsWith(group)) {
        for (pos=group.length();pos<name.length();pos++)
          if (name.charAt(pos)=='/')
            break;
        if (pos!=name.length()) { //found a /, so return the group, not variable
          name=name.substring(0,pos+1); //find just the group
          for (k=0;k<i;k++) //has this group been found before?
            if (((String)regName.elementAt(k)).startsWith(name))
              break;   //this group has been found before
          if (k<i)     //if this group found before then
            name=null; //don't output it again
        }
      } else
        name=null; //if group is not a prefix of name, don't add it
      if (name!=null) //if found a variable or a new group
        vars.addElement((Object)name);
    }
    v=new String[vars.size()];      //copy new vector into an array...
    vars.copyInto(v);
    return v;                          //...and return the array
  } //end getVars

  /** Watchable objects call this when ready for their variables to be seen.
    * update() should only be called when all the Watchable objects that have
    * registered with this object are ready to be seen.  update() in turn
    * calls the update() method of the Watcher objects when the appropriate
    * variables have changed (or changed for the nth time).
    */
  public synchronized void update() {
    long startThisUpdateTime=System.currentTimeMillis();
    if (name!=null) //if there is at least one variable being watched...
      for (int i=0;i<name.length;i++) { //check each watched variable
        if (watcher[i]!=null && freq[i].val>0) //if this watch hasn't been unregistered, and isn't being ignored
          if (prevVar[i]==null) { //if first time this variable has been seen
            if (var[i]==null)
              System.out.println("variable "+name[i]+" not initialized");
            prevVar[i]=(Pointer)var[i].clone();  //first copy the pointer
            var[i].copyInto(prevVar[i]);         //then fill in the pointer with what it points to
            changes[i]=0;                                 //do same things as if it was time for an update
            watcher[i].update(name[i],var[i],watched[i]); //do same things as if it was time for an update
          } else if (!var[i].equalVal(prevVar[i])) { //if variable changed since last time
            changes[i]++;
            var[i].copyInto(prevVar[i]);
            if (freq[i].val>0 && changes[i]>=freq[i].val) {//call update every freq steps
              changes[i]=0;
              watcher[i].update(name[i],var[i],watched[i]);
            }
          }
      }

    // be polite on nonpreemptive threading systems:
    Thread.currentThread().yield(); //always yield every time
    numUpdates++;                   //only sleep periodically, if at all
    if (sleepFreq.val>0 && numUpdates>=sleepFreq.val) { //time to sleep and yield
      numUpdates=0;
      if (sleepTime.val>0)
        try {
          Thread.sleep(sleepTime.val);
        } catch (InterruptedException e) {
        }
    }

    //profile how much time is spent in Update(), including other threads in yield():
    long currTime=System.currentTimeMillis();
    updateTime+=currTime-startThisUpdateTime; //how long did this method take?
    if (currTime-startTime>1000*profilePeriod.val) {//every profilePeriod seconds, start over on profiling
      updatePercent.val=100 * updateTime/(0.+currTime-startTime); //find % of time wasted updating
      updateTime=0;
      startTime=currTime;
    }
  }//end update

  /** A Watcher calls this to get a Pointer to a watchable variable
    * given its name.
    * The Watchable object must have already registered the variable.
    * This returns the pointer if successful, and null if it couldn't find
    * the requested variable.
    */
  public synchronized Pointer findVar(String varName) {
    if (varName.equals(""))
      return null;
    for (int i=0;i<regName.size();i++)
      if (((String)regName.elementAt(i)).equals(varName)) {
        return (Pointer)regPtr.elementAt(i);
      }
    System.out.println("In WatchManager.findVar, variable '"+varName+"' does not exist or is not watchable");
    throw new NullPointerException();/**/
  }

  /** A Watcher calls this to be notified when variable name changes.
    * It is notified by having its update() method called.  If freq>1
    * then it is called at the beginning, and once every freq changes
    * thereafter. If freq<1, then update() is never called, but
    * unregister() is still called when the variable is unregistered.
    * This returns the pointer if successful, and null if it couldn't find
    * the requested variable.
    */
  public synchronized Pointer registerWatch(String varName, PInt varFreq, Watcher varWatcher) {
    String    newName[];
    Watcher   newWatcher[];
    Pointer   newVar[];
    Pointer   newPrevVar[];
    PInt      newFreq[];
    int       newChanges[];
    Watchable newWatched[];
    for (int i=0;i<regName.size();i++)  { //find this variable name
      if (((String)regName.elementAt(i)).equals(varName)) { //this is the one to watch
        newName   =new String   [name.length+1];
        newWatcher=new Watcher  [name.length+1];
        newVar    =new Pointer  [name.length+1];
        newPrevVar=new Pointer  [name.length+1];
        newFreq   =new PInt     [name.length+1];
        newChanges=new int      [name.length+1];
        newWatched=new Watchable[name.length+1];
        for (int k=0;k<name.length;k++) {
          newName   [k]=name   [k];
          newWatcher[k]=watcher[k];
          newVar    [k]=var    [k];
          newPrevVar[k]=prevVar[k];
          newFreq   [k]=freq   [k];
          newChanges[k]=changes[k];
          newWatched[k]=watched[k];
        }
        name   =newName;
        watcher=newWatcher;
        var    =newVar;
        prevVar=newPrevVar;
        freq   =newFreq;
        changes=newChanges;
        watched=newWatched;
        name   [name.length-1]=varName;
        watcher[name.length-1]=varWatcher;
        freq   [name.length-1]=varFreq;
        changes[name.length-1]=0;
        prevVar[name.length-1]=null;
        var    [name.length-1]=(Pointer)  regPtr.elementAt(i);
        watched[name.length-1]=(Watchable)regWatchable.elementAt(i);

        return var[name.length-1]; //return pointer to var being watched
      } //end if elementat = var.name
    } //end for i
    return null;
  } //end registerWatch

  /** A Watcher calls this if it no longer cares about this variable.
    * It will subsequently not be called when the variable changes
    * or is unregistered by the Watchable object.
    */
  public synchronized void unregisterWatch(String nm, Watcher wtchr) {
    for (int i=0;i<name.length;i++) { //for each watch currently registered
      if (watcher[i]==wtchr && name[i].equals(nm)) //if this is the watch
        watcher[i]=null;
    }
    compactWatches(); //recreate the array of watches without the null elements
  }

  /** Watchable objects call this to unregister a variable. */
  public synchronized void unregisterVar(String varName) {
    for (int i=0;i<regName.size();i++)
      if (((String)regName.elementAt(i)).equals(varName)) {
        regPtr.removeElementAt(i);
        regName.removeElementAt(i);
        regWatchable.removeElementAt(i);
      }
    for (int i=0;i<name.length;i++)  //for each watch currently registered
      if (name[i].equals(varName) && watcher[i]!=null) { //unregister all watchers of this var
        watcher[i].unregister(varName); //tell watcher it no longer exists
        watcher[i]=null;
      }
    compactWatches(); //recreate the array of watches without the null elements
  }

  /** Watchable objects call this to register a variable that's a pointer to an object.
    * Note: watchers are given a pointer to the variable.  Typically,
    * watchers only watch (hence the name), but in some cases
    * a watcher can follow the pointer and change the value of the
    * variable.  If a watchable object has a variable X that should
    * be watchable but not changeable by a watcher, then it should
    * declare X and PX internally (the latter of type Pointer),
    * register PX instead of X, and then update it with
    * PX.val=x periodically (perhaps right before each call
    * to update()).  That ensures that no watcher can change
    * X, but they'll get to see a copy of its value.
    * This trick can also allow the watchable object to
    * access its own watchable variables in an inner loop
    * slightly faster (saving one pointer dereference), since
    * it can work with X rather than PX.val.
    */
  public synchronized void registerVar(String name,Pointer v,Watchable obj) {
    regName.addElement     ((Object)name);
    regWatchable.addElement((Object)obj);
    regPtr.addElement      ((Object)v);
  }

  /** unregister every watch associated with this watcher */
  public synchronized void unregisterWatcher(Watcher wtchr) {
    for (int i=0;i<name.length;i++) { //for each watch currently registered
      if (watcher[i]==wtchr) //if this is the watch
        watcher[i]=null;
    }

    compactWatches(); //recreate the array of watches without the null elements
  }

  /** unregister every variable assocaited with this watchable */
  public synchronized void unregisterWatchable(Watchable watchable) {
    String varName=null;
    for (int i=0;i<regName.size();i++) //for each registered variable
      if (((Watchable)regWatchable.elementAt(i))==watchable) { //if it belongs to this Watchable
        varName=(String)regName.elementAt(i); //a variable belonging to watchable
        for (int j=0;j<name.length;j++)  //for each watch currently registered
          if (name[j].equals(varName) && watcher[j]!=null) { //unregister all watchers of this var
            watcher[j].unregister(varName); //tell watcher it no longer exists
            watcher[j]=null;
          }
        regPtr.removeElementAt(i);      //now unregister the variable itself
        regName.removeElementAt(i);
        regWatchable.removeElementAt(i);
        i--;  //now there's a new one at position i, so undo the end-of-loop increment of i
      }
    compactWatches(); //recreate the array of watches without the null elements
  }//end unregisterWatchable

  /** recreate the arrays for the watches without the null elements (deleted watches) */
  private void compactWatches() {
    int num=0; //number of active watches
    for (int i=0;i<watcher.length;i++)
      if (watcher[i]!=null)
        num++;

    if (num==watcher.length) //if no need to compact, then quit
      return;

    String   [] newName   =new String   [num];
    Watcher  [] newWatcher=new Watcher  [num];
    Pointer  [] newVar    =new Pointer  [num];
    Pointer  [] newPrevVar=new Pointer  [num];
    PInt     [] newFreq   =new PInt     [num];
    int      [] newChanges=new int      [num];
    Watchable[] newWatched=new Watchable[num];

    int k=0;
    for (int i=0;i<watcher.length;i++) {
      if (watcher[i]!=null) {
          newName   [k]=name   [i];
          newWatcher[k]=watcher[i];
          newVar    [k]=var    [i];
          newPrevVar[k]=prevVar[i];
          newFreq   [k]=freq   [i];
          newChanges[k]=changes[i];
          newWatched[k]=watched[i];
          k++;
      }
    }
    name   =newName;
    watcher=newWatcher;
    var    =newVar;
    prevVar=newPrevVar;
    freq   =newFreq;
    changes=newChanges;
    watched=newWatched;
  }
}//end WatchManager
