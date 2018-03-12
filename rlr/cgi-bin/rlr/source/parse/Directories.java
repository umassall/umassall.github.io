package parse;
import java.util.Hashtable;

import Random;       /**/
import Events;       /**/
import ProjWin;      /**/
import GWin;         /**/
import Project;      /**/
import WebSim;       /**/
import FindBNF;      /**/
import Logo;         /**/
import TestMatrix;   /**/
import Credits;      /**/
import Display;      /**/
import DisplayList;  /**/
import ShowEdit;     /**/
import ShowThreads;  /**/
import Title;        /**/

/** This is a list of all the directories that parser.Parser should look
  * in.  It should include all directories containing Parsable objects.
  * It is also used by FindBNF.
  *    <p>This code is (c) 1996,97 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.2 3 July 97
  * @author Leemon Baird
  */
public class Directories {
  //v. 1.2 3 July 97 Added hack to preload all classes so it works on all platforms
  //v. 1.1 23 May 97
  //v. 1.0 10 March 96

  //The following is the ugliest imaginable hack.  To generate a Class
  //object for a desired class, given only its name in a string, you
  //ought to be able to say:
  //           new("MyClass")
  //But the JDK1.0, JDK1.1, and Symantec1.0 compilers all say
  //"this feature not implemented", even though it's clearly defined
  //in the 1.0 spec.  So, as a workaround, you can say instead:
  //           Class.ForName("MyClass").newInstance()
  //which the 1.0 spec explicitly says will load the class if it isn't
  //already loaded.  That works fine on Win95 and WinNT under JDK1.0, JDK1.1,
  //Cafe, Netscape, and MSIExplorer.  Unfortunately, it gives a security violation
  //on SunOs4, Linux, MacOS, and SVR4.  So, to run on those platforms, you
  //must first force a load of every class that you think you might consider
  //instantiating someday, which wastes lots of memory that under all current
  //implementations will never be garbage collected.  And, if you forget one
  //line in the following, that object will cause security faults on all
  //platforms other than Windows.
  public static Random                              t2  =null; /**/
  public static Events                              t3  =null; /**/
  public static ProjWin                             t4  =null; /**/
  public static GWin                                t5  =null; /**/
  public static Project                             t6  =null; /**/
  public static WebSim                              t7  =null; /**/
  public static FindBNF                             t8  =null; /**/
  public static Logo                                t9  =null; /**/
  public static TestMatrix                          t10 =null; /**/
  public static Credits                             t11 =null; /**/
  public static Display                             t12 =null; /**/
  public static DisplayList                         t13 =null; /**/
  public static ShowEdit                            t14 =null; /**/
  public static ShowThreads                         t15 =null; /**/
  public static Title                               t16 =null; /**/
  public static expression.IntExp                   t17 =null; /**/
  public static expression.NumExp                   t18 =null; /**/
  public static expression.IntVarExp                t19 =null; /**/
  public static expression.NumVarExp                t20 =null; /**/
  public static expression.NumVarTerm               t21 =null; /**/
  public static expression.NumVarFact               t22 =null; /**/
  public static fix.Util                            t24 =null; /**/
  public static matrix.MatrixD                      t25 =null; /**/
  public static matrix.MatrixException              t26 =null; /**/
  public static matrix.MatrixF                      t27 =null; /**/
  public static parse.Directories                   t28 =null; /**/
  public static parse.Parser                        t29 =null; /**/
  public static parse.Parsable                      t30 =null; /**/
  public static parse.ParserException               t31 =null; /**/
  public static parse.Scanner                       t32 =null; /**/
  public static parse.Unparser                      t33 =null; /**/
  public static picture.Animation                   t34 =null; /**/
  public static picture.Antialias                   t35 =null; /**/
  public static picture.ColorMap                    t36 =null; /**/
  public static picture.ColorMapEntry               t37 =null; /**/
  public static picture.Colors                      t38 =null; /**/
  public static picture.ColorVector                 t39 =null; /**/
  public static picture.Description                 t40 =null; /**/
  public static picture.Edges                       t41 =null; /**/
  public static picture.FadeIn                      t42 =null; /**/
  public static picture.Gallery                     t43 =null; /**/
  public static picture.PicPipe                     t44 =null; /**/
  public static picture.PicPipeList                 t45 =null; /**/
  public static picture.PicPipePipeline             t46 =null; /**/
  public static picture.Picture                     t47 =null; /**/
  public static picture.Region                      t48 =null; /**/
  public static picture.RndColor                    t49 =null; /**/
  public static picture.ValueMap                    t50 =null; /**/
  public static picture.ValueMapEntry               t51 =null; /**/
  public static picture.directFractal.DirectFractal t52 =null; /**/
  public static picture.directFractal.Fract1        t53 =null; /**/
  public static picture.directFractal.Maze          t54 =null; /**/
  public static pointer.PArray                      t55 =null; /**/
  public static pointer.PBoolean                    t56 =null; /**/
  public static pointer.PByte                       t57 =null; /**/
  public static pointer.PChar                       t58 =null; /**/
  public static pointer.PDouble                     t59 =null; /**/
  public static pointer.PDoubleArray1D              t60 =null; /**/
  public static pointer.PFloat                      t61 =null; /**/
  public static pointer.PFloatArray1D               t62 =null; /**/
  public static pointer.PInt                        t63 =null; /**/
  public static pointer.PLong                       t64 =null; /**/
  public static pointer.PMatrixD                    t65 =null; /**/
  public static pointer.PObject                     t66 =null; /**/
  public static pointer.Pointer                     t67 =null; /**/
  public static pointer.PShort                      t68 =null; /**/
  public static pointer.PString                     t69 =null; /**/
  public static sim.Experiment                      t70 =null; /**/
  public static sim.ForExperiment                   t71 =null; /**/
  public static sim.Simulator                       t72 =null; /**/
  public static sim.TDLambda                        t73 =null; /**/
  public static sim.data.Data                       t74 =null; /**/
  public static sim.data.Dot                        t75 =null; /**/
  public static sim.data.OnesRnd                    t76 =null; /**/
  public static sim.data.RndOnes                    t77 =null; /**/
  public static sim.data.RndCircle                  t78 =null; /**/
  public static sim.data.RndDisk                    t79 =null; /**/
  public static sim.data.RndUniformLine             t80 =null; /**/
  public static sim.data.RndUniformSquare           t81 =null; /**/
  public static sim.data.Table                      t82 =null; /**/
  public static sim.data.XOR                        t83 =null; /**/
  public static sim.data.RemoteTable                t84 =null; /**/
  public static sim.data.SpiralData                 t85 =null; /**/
  public static sim.display.Contour                 t86 =null; /**/
  public static sim.display.Graph2D                 t87 =null; /**/
  public static sim.display.Graph3D                 t88 =null; /**/
  public static sim.display.Grid                    t89 =null; /**/
  public static sim.display.PlotXY                  t90 =null; /**/
  public static sim.display.SaveDisplay             t91 =null; /**/
  public static sim.errFun.AdvantageLearning        t92 =null; /**/
  public static sim.errFun.ErrFun                   t93 =null; /**/
  public static sim.errFun.LocalLearning            t94 =null; /**/
  public static sim.errFun.PDFE                     t95 =null; /**/
  public static sim.errFun.QLearning                t96 =null; /**/
  public static sim.errFun.SupervisedLearning       t97 =null; /**/
  public static sim.errFun.Batch                    t98 =null; /**/
  public static sim.errFun.ErrAvg                   t99 =null; /**/
  public static sim.errFun.ReinforcementLearning    t100=null; /**/
  public static sim.errFun.ValueIteration           t101=null; /**/
  public static sim.funApp.FunApp                   t102=null; /**/
  public static sim.funApp.Net                      t103=null; /**/
  public static sim.funApp.ValuePolicy              t104=null; /**/
  public static sim.funApp.LookupTable              t105=null; /**/
  public static sim.funApp.SpiralFunction           t106=null; /**/
  public static sim.funApp.InterferenceFunction     t107=null; /**/
  public static sim.gradDesc.Backprop               t108=null; /**/
  public static sim.gradDesc.ConjGrad               t109=null; /**/
  public static sim.gradDesc.GradDesc               t110=null; /**/
  public static sim.gradDesc.IDD                    t111=null; /**/
  public static sim.mdp.GridWorld                   t112=null; /**/
  public static sim.mdp.Hall                        t113=null; /**/
  public static sim.mdp.LQR                         t114=null; /**/
  public static sim.mdp.MDP                         t115=null; /**/
  public static sim.mdp.XORmdp                      t116=null; /**/
  public static sim.mdp.HCDemo                      t117=null; /**/
  public static watch.Watchable                     t118=null; /**/
  public static watch.WatchManager                  t119=null; /**/
  public static watch.Watcher                       t120=null; /**/



  /** A complete list of all packages in WebSim.  This list must be complete
    * and accurate in order for WebSim to work correctly.
    */
  public static final String[] dirs=
    { "",
      "sim.",
      "sim.display.",
      "sim.mdp.",
      "sim.data.",
      "sim.gradDesc.",
      "sim.errFun.",
      "sim.funApp.",
      "picture.",
      "picture.directFractal.",
      "matrix.",
      "expression.",
      "pointer."
    };

  /** the package names for classes that have been parsed so far */
  public static Hashtable parsedClasses=new Hashtable();

  /** Some guesses as to the package name for some classes.  The program
    * will work correctly, though more slowly, if some classes are missing
    * or have the wrong package name listed, or have a name listed that isn't
    * even an existing package.
    */
  public static Hashtable packageHints=new Hashtable();

  //initialize the packageHints table with as many of the package names
  //as possible to speed up searches.  The parsing will still work correctly
  //if this list has errors or omissions, but parsing is faster if it is
  //correct and complete.
  static {
    packageHints.put("Random",               "");
    packageHints.put("Events",               "");
    packageHints.put("ProjWin",              "");
    packageHints.put("GWin",                 "");
    packageHints.put("Project",              "");
    packageHints.put("WebSim",               "");
    packageHints.put("FindBNF",              "");
    packageHints.put("Logo",                 "");
    packageHints.put("TestMatrix",           "");
    packageHints.put("Credits",              "");
    packageHints.put("Display",              "");
    packageHints.put("DisplayList",          "");
    packageHints.put("ShowEdit",             "");
    packageHints.put("ShowThreads",          "");
    packageHints.put("Title",                "");
    packageHints.put("IntExp",               "expression.");
    packageHints.put("NumExp",               "expression.");
    packageHints.put("IntVarExp",            "expression.");
    packageHints.put("NumVarExp",            "expression.");
    packageHints.put("NumVarTerm",           "expression.");
    packageHints.put("NumVarFact",           "expression.");
    packageHints.put("Expression",           "expression.");
    packageHints.put("Util",                 "fix.");
    packageHints.put("MatrixD",              "matrix.");
    packageHints.put("MatrixException",      "matrix.");
    packageHints.put("MatrixF",              "matrix.");
    packageHints.put("Directories",          "parse.");
    packageHints.put("Parser",               "parse.");
    packageHints.put("Parsable",             "parse.");
    packageHints.put("ParserException",      "parse.");
    packageHints.put("Scanner",              "parse.");
    packageHints.put("Unparser",             "parse.");
    packageHints.put("Animation",            "picture.");
    packageHints.put("Antialias",            "picture.");
    packageHints.put("ColorMap",             "picture.");
    packageHints.put("ColorMapEntry",        "picture.");
    packageHints.put("Colors",               "picture.");
    packageHints.put("ColorVector",          "picture.");
    packageHints.put("Description",          "picture.");
    packageHints.put("Edges",                "picture.");
    packageHints.put("FadeIn",               "picture.");
    packageHints.put("Gallery",              "picture.");
    packageHints.put("PicPipe",              "picture.");
    packageHints.put("PicPipeList",          "picture.");
    packageHints.put("PicPipePipeline",      "picture.");
    packageHints.put("Picture",              "picture.");
    packageHints.put("Region",               "picture.");
    packageHints.put("RndColor",             "picture.");
    packageHints.put("ValueMap",             "picture.");
    packageHints.put("ValueMapEntry",        "picture.");
    packageHints.put("DirectFractal",        "picture.directFractal.");
    packageHints.put("Fract1",               "picture.directFractal.");
    packageHints.put("Maze",                 "picture.directFractal.");
    packageHints.put("PArray",               "pointer.");
    packageHints.put("PBoolean",             "pointer.");
    packageHints.put("PByte",                "pointer.");
    packageHints.put("PChar",                "pointer.");
    packageHints.put("PDouble",              "pointer.");
    packageHints.put("PDoubleArray1D",       "pointer.");
    packageHints.put("PFloat",               "pointer.");
    packageHints.put("PFloatArray1D",        "pointer.");
    packageHints.put("PInt",                 "pointer.");
    packageHints.put("PLong",                "pointer.");
    packageHints.put("PMatrixD",             "pointer.");
    packageHints.put("PObject",              "pointer.");
    packageHints.put("Pointer",              "pointer.");
    packageHints.put("PShort",               "pointer.");
    packageHints.put("PString",              "pointer.");
    packageHints.put("Experiment",           "sim.");
    packageHints.put("ForExperiment",        "sim.");
    packageHints.put("Simulator",            "sim.");
    packageHints.put("TDLambda",             "sim.");
    packageHints.put("Data",                 "sim.data.");
    packageHints.put("Dot",                  "sim.data.");
    packageHints.put("OnesRnd",              "sim.data.");
    packageHints.put("RndOnes",              "sim.data.");
    packageHints.put("RndCircle",            "sim.data.");
    packageHints.put("RndDisk",              "sim.data.");
    packageHints.put("RndUniformLine",       "sim.data.");
    packageHints.put("RndUniformSquare",     "sim.data.");
    packageHints.put("Table",                "sim.data.");
    packageHints.put("XOR",                  "sim.data.");
    packageHints.put("RemoteTable",          "sim.data.");
    packageHints.put("SpiralData",           "sim.data.");
    packageHints.put("Contour",              "sim.display.");
    packageHints.put("Graph2D",              "sim.display.");
    packageHints.put("Graph3D",              "sim.display.");
    packageHints.put("Grid",                 "sim.display.");
    packageHints.put("PlotXY",               "sim.display.");
    packageHints.put("SaveDisplay",          "sim.display.");
    packageHints.put("AdvantageLearning",    "sim.errFun.");
    packageHints.put("ErrFun",               "sim.errFun.");
    packageHints.put("LocalLearning",        "sim.errFun.");
    packageHints.put("PDFE",                 "sim.errFun.");
    packageHints.put("QLearning",            "sim.errFun.");
    packageHints.put("SupervisedLearning",   "sim.errFun.");
    packageHints.put("Batch",                "sim.errFun.");
    packageHints.put("ErrAvg",               "sim.errFun.");
    packageHints.put("ReinforcementLearning","sim.errFun.");
    packageHints.put("ValueIteration",       "sim.errFun.");
    packageHints.put("FunApp",               "sim.funApp.");
    packageHints.put("Net",                  "sim.funApp.");
    packageHints.put("ValuePolicy",          "sim.funApp.");
    packageHints.put("LookupTable",          "sim.funApp.");
    packageHints.put("SpiralFunction",       "sim.funApp.");
    packageHints.put("InterferenceFunction", "sim.funApp.");
    packageHints.put("Backprop",             "sim.gradDesc.");
    packageHints.put("ConjGrad",             "sim.gradDesc.");
    packageHints.put("GradDesc",             "sim.gradDesc.");
    packageHints.put("IDD",                  "sim.gradDesc.");
    packageHints.put("GridWorld",            "sim.mdp.");
    packageHints.put("Hall",                 "sim.mdp.");
    packageHints.put("LQR",                  "sim.mdp.");
    packageHints.put("MDP",                  "sim.mdp.");
    packageHints.put("XORmdp",               "sim.mdp.");
    packageHints.put("HCDemo",               "sim.mdp.");
    packageHints.put("Watchable",            "watch.");
    packageHints.put("WatchManager",         "watch.");
    packageHints.put("Watcher",              "watch.");
  }//end static
} //end class directories
