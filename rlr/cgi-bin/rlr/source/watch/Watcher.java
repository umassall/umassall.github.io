package watch;
import pointer.*;

/** A Watcher can get the names of watchable variables from a
  * WatchManager, and call registerWatch to ask to be notified when
  * a variable changes (or changes every nth time).  The WatchManager
  * will call update() when that happens, so the Watcher can display
  * or record the appropriate variables.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 3 June 96
  * @author Leemon Baird
  */
public interface Watcher {
  /** One of the watched variables has changed, so look at it and others.
    * This is called by the WatchManager.
    */
  public void update(String changedName, Pointer changedVar, Watchable obj);

  /** One of the watched variables has been unregistered.
    * The watcher doesn't have to do anything, but might
    * remove this variable from some internal data structure.
    * This is called by the WatchManager.
    */
  public void unregister(String watchedVar);
}
