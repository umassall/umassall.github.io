import parse.*;
import java.awt.*;

/** Show all threads in a window
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 25 July 96
  * @author Leemon Baird
  */
public class ShowThreads extends Project {
  private int line; //the line of text being written to now

  /* Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "//Show all threads in a window.";
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    return this;
  } //end method parse

  /** This project does nothing, so the Thread dies immediately */
  public void run() {}

  /** show the threads */
  public void paint(Graphics g) {
    if (g!=null) {
      listAllThreads(g,10,15);
      repaint(1000);
    }
  }

  //print out all the threads that now exist
  private void listAllThreads(Graphics g,int indentDist,int height) {
    ThreadGroup current_thread_group,root_thread_group,parent;
    line=height;
    g.drawString("-------------------------------------",0,line);
    line+=height;
    current_thread_group=Thread.currentThread().getThreadGroup();
    root_thread_group=current_thread_group;
    parent=root_thread_group.getParent();
    while(parent!=null){
      root_thread_group=parent;
      parent=parent.getParent();
    }
    list_group(g,0,root_thread_group,indentDist,height);
    g.drawString("-------------------------------------",0,line);
    line+=height;
  }

  //print out all the threads and groups in a given thread group
  private void list_group(Graphics g,int indent,
                          ThreadGroup gr,int indentDist,int height) {
    if (gr==null)
      return;
    int num_threads=gr.activeCount();
    int num_groups =gr.activeGroupCount();
    Thread[] threads=new Thread[num_threads];
    ThreadGroup[] groups=new ThreadGroup[num_groups];
    gr.enumerate(threads,false);
    gr.enumerate(groups,false);
    g.drawString("["+/*"Thread Group: "+*/gr.getName()+" "+
                 /*"Max Priority: "+*/gr.getMaxPriority()+
                 (gr.isDaemon()?" Daemon":"")+"]",indent,line);
    line+=height;
    for (int i=0;i<num_threads;i++) {
      if (threads[i]!=null) {
        g.drawString(/*"Thread: "+*/threads[i].getName()+
                     " "+/*"Priority: "+*/threads[i].getPriority()+
                     (threads[i].isDaemon()?" Daemon ":"")+
                     (threads[i].isAlive()?"":" Dead "),
                     indent+indentDist,line);
        line+=height;
      }
    }
    for (int i=0;i<num_groups;i++)
      list_group(g,indent+indentDist,groups[i],indentDist,height);
  }
} // end class Simulator
