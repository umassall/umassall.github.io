package watch;

/** A Watchable object is one that registers some of its variables
  * with a WatchManager, and then periodically allows the WatchManager
  * to activate the various Watcher objects so they can get copies
  * of the variables in the Watchable object, perhaps to record or
  * display them. The WatchManager should be in the same
  * thread as the Watchable object, but each Watcher may have a separate
  * thread.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 3 June 96
  * @author Leemon Baird
  */
public interface Watchable {
  /** Register all variables with this WatchManager.
    * This method should register all the variables in this object and
    * in those it links to.  The name of each variable should be
    * appended to the end of the String name.
    */
  public abstract void setWatchManager(WatchManager wm,String name);

  /** Return the WatchManager set by setWatchManager(). */
  public abstract WatchManager getWatchManager();

  /** Return the variable "name" that was passed into setWatchManager */
  public abstract String getName();
}
