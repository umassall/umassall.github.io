import watch.*;
import Display;
import matrix.*;
import parse.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import expression.*;
import pointer.*;

/** send all BNF strings to standard out
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 24 Aug 96
  * @author Leemon Baird
  */
public class FindBNF extends Project {
  private final boolean showBlankTypes=true; //have a line for a type that has no children
  private String dirName=null;         //name of directory to print out
  private IntExp maxLHS,maxRHS,maxCom; //widths of 3 columns
  private boolean html=false;          //should HTML format (and documentation) be used?
  private boolean summary=false;       //should abbreviated comments be used?
  private MatrixD tempD=new MatrixD(); //force a load and the native linking message to appear
  private MatrixF tempF=new MatrixF(); //force a load and the native linking message to appear

  /* Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "['html'] "+
           "['summary'] "+
           "'codebase' <string> "+
           "'widths' IntExp IntExp IntExp"+
           "//Find BNF for all objects. "+
           "Send BNFs to standard out in 3 columns with these widths, "+
           "optionally with HTML headers and documentation. "+
           "All files in directory codebase and below are explored. "+
           "If 'summary' is there, comments are less detailed "+
           "(only print out up to the first period).";
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    if (html)
      u.emit("html ");
    if (summary)
      u.emit("summary ");
    u.emit("codebase ");
    u.emit(dirName); u.emit(" ");
    u.emit("widths ");
    u.emitUnparse(maxLHS,lang);
    u.emitUnparse(maxRHS,lang);
    u.emitUnparse(maxCom,lang);
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    html=p.parseID("html",false);
    summary=p.parseID("summary",false);
    p.parseID("codebase",true);
    dirName=p.parseString(true);
    p.parseID("widths",true);
    maxLHS=(IntExp)p.parseClass("IntExp",lang,true);
    maxRHS=(IntExp)p.parseClass("IntExp",lang,true);
    maxCom=(IntExp)p.parseClass("IntExp",lang,true);
    return this;
  } //end method parse

  FilenameFilter classFilter=new ClassFilter(); //return only .class files
  FilenameFilter dirFilter  =new DirFilter();   //return only directories
  Vector name   =new Vector(50,10); //unbracketed nonterminals
  Vector type   =new Vector(50,10); //bracketed nonterminals
  Vector rhs    =new Vector(50,10); //right hand side of each production
  Vector comment=new Vector(50,10); //comments for each production
  Vector classes=new Vector(50,10); //the Class for each unbracketed nonterminal
  String[] types; //the type Vector converted to an array of Strings

  /** Print all the BNF strings */
  public void run() {
    String temp;
    int start;
    if (html) { //HTML format includes header and explanation
      System.out.println("<html>");
      System.out.println("<title>BNF for WebSim code embedded in Web Pages</title>");
      System.out.println("<h1 align=center><img hspace=20 vspace=0 border=0 align=center src=images/logob.gif>"+
                         "BNF for WebSim "+(summary?"(short Comments)":"")+"</h1>");
      System.out.println("WebSim parses a string embedded in a Web page.  This string uses ");
      System.out.println("the language given below to describe the program to run.  This BNF listing");
      System.out.println("was automatically generated from the BNF() methods in each class. The");
      System.out.println("start symbol is &quotWebSim&quot.");
      System.out.println("<p>");
      System.out.println("<pre>");
    }

    type.addElement("string"); //predefined types
    type.addElement("integer");
    type.addElement("double");
    type.addElement("boolean");

    checkFiles("",new File(dirName));

    types=new String[type.size()];
    type.copyInto(types);
    quicksort(types,4,types.length-1);
    for (int i=4;i<types.length;i++) { //print production for each type
      if (showBlankTypes) {
        print("<"+types[i]+">");
        spaces(maxLHS.val-types[i].length()-2);
        print(" ::= ");
      }
      try {
        Class c=Class.forName(types[i]); //find the class for this type, if any
        boolean first=true; //is this the first match found?
        for (int k=0;k<classes.size();k++) {//print all nonabstract Parsable classes of this type
          if (Parser.isOfType((Class)classes.elementAt(k),c)) {
            if (first) {
              first=false;
              if (!showBlankTypes) {
                print("<"+types[i]+">");
                spaces(maxLHS.val-types[i].length()-2);
                print(" ::= ");
              }
            } else {
              println(" |");
              spaces(maxLHS.val+5);
            }
            temp=(String)name.elementAt(k);
            for (start=temp.length()-1; //strip all but the last name after a dot
                 start>=0;
                 start--)
              if (temp.charAt(start)=='.')
                break;
            print("'"+temp.substring(start+1,temp.length())+"'");
            spaces(maxRHS.val-temp.length()+start+1);
            print(""+temp);
          }//end if Parser.isOfType
        } //end for k
      } catch (ClassNotFoundException e) {  //IntExp, <string>, etc. don't really exist
      } catch (Throwable e) { //this shouldn't happen
        println(e.getMessage());
        e.printStackTrace();
      }
      println();
    }//end for i, print all productions
    print("<string> ");
    spaces(maxLHS.val-9);
    println(" ::= a string in single or double quotes");
    print("<integer>");
    spaces(maxLHS.val-9);
    println(" ::= an integer (no decimal point)");
    print("<double> ");
    spaces(maxLHS.val-9);
    println(" ::= a floating point number with a decimal point");
    print("<boolean>");
    spaces(maxLHS.val-9);
    println(" ::= 'true' | 'false'");
    if (html) { //HTML format includes header and explanation
      System.out.println("</pre>");
      System.out.println("</html>");
    }
    closeWindow(); //close the window when this Project is done
  }//end run

  //print these three strings in three columns with ::= and // between
  private void printCols(String c1, String c2, String c3) {
    boolean first=true; //is it printing the first line?
    int brk;
    if (c1==null) c1="";
    if (c2==null) c2="";
    if (c3==null) c3="";
    while (c1.length()+c2.length()+c3.length()>0) {
      if (!first) {
        c1="   "+c1; //indent lines after the first one
      }
      if (c1.length() <= maxLHS.val) {
        print(c1);
        spaces(maxLHS.val-c1.length());
        c1="";
      } else { //wrap at a dot, if possible
        for (brk=maxLHS.val-1; brk>4 && c1.charAt(brk)!='.';brk--);
        if (brk<=4) //if no dots, just break at the boundary
          brk=maxLHS.val;
        print(c1.substring(0,brk));  //print up to not including dot
        spaces(maxLHS.val-brk);
        c1=c1.substring(brk,c1.length());       //leftovers go on next line
      }

      if (first) {
        first=false;
        print(" ::= ");
      } else if (summary || c2!="")
        print("     ");

      if (!summary && c2!="" && c2.length()<=maxRHS.val) {
        print(c2);
        c2="";
        println();
        spaces(maxLHS.val);
      } else if (!summary && c2=="") {
      } else if (c2.length() <= maxRHS.val) {
        print(c2);
        spaces(maxRHS.val-c2.length());
        c2="";
      } else { //wrap at a space, if possible
        for (brk=maxRHS.val-1; brk>0 && c2.charAt(brk)!=' ';brk--);
        if (brk==0) //if no spaces, just break at the boundary
          brk=maxRHS.val-1;
        print(c2.substring(0,brk+1));  //print up to and including space
        spaces(maxRHS.val-brk-1);
        c2=c2.substring(brk+1,c2.length());       //leftovers go on next line
      }

      if (summary || c2=="") {
        if (c3.length()>0)
          print(" // ");
        int pos=c3.indexOf("\n");
        if (pos>-1 && pos <= maxCom.val) { //newline in comments at double quote character
          print(c3.substring(0,pos));  //print up to the break
          c3=c3.substring(pos+1,c3.length()); //rest goes on the next line
        } else if (c3.length() <= maxCom.val) { //if it all fits on one line, print it
          print(c3);
          c3="";
        } else { //wrap at a space, if possible
          for (brk=maxRHS.val-1; brk>0 && c3.charAt(brk)!=' ';brk--);
          if (brk<3) //if no spaces, just break at the boundary
            brk=maxCom.val-1;
          print(c3.substring(0,brk+1));  //print up to and including space
          c3=c3.substring(brk+1,c3.length());       //leftovers go on next line
        }
      }
      println();
    }//end while
  }//end printCols


  //print BNF string for all files starting in the specified directory,
  //which all have the specified prefix representing their packages.
  private void checkFiles(String pkg,File currDir) {
    Class clss;
    Parsable p;
    Object obj;

    String[] files;
    String s1,s2,s3;
    StringBuffer currType=new StringBuffer();
    int comPos; //starting position of the comment
    int len;    //length so far of current line being printed
    int line;   //which line is being printed (top is 0)

    if (currDir.isDirectory()) {
      files=currDir.list(classFilter);
      if (files != null) {
        quicksort(files,0,files.length-1);
        for (int i=0;i<files.length;i++)
          try {
            files[i]=files[i].substring(0,files[i].length()-6);
            s1=pkg+files[i];
            clss=Class.forName(s1);
            obj=clss.newInstance();
            if (obj instanceof Parsable) {
              classes.addElement(clss);
              name.addElement(s1);
              p=(Parsable)obj;
              s2=getBNF(p,1);
              s3=null;
              comPos=s2.indexOf("//");
              if (comPos>-1) { //a comment was found
                s3=s2.substring(comPos+2); //get the comment
                s2=s2.substring(0,comPos); //strip off the comment
                rhs.addElement(s2);
                comment.addElement(s3);
                if (summary) { //print comment only up to first dot
                  comPos=s3.indexOf(".");
                  if (comPos>-1) //a dot was found in the comment
                    s3=s3.substring(0,comPos+1);
                }
          }
              printCols(s1,s2,s3);
              currType.setLength(0);
              len=0;
              line=0;
              boolean inStr=false; //is parsing within a string?
              boolean inType=false; //is parsing within a type?
              char c; //next character of string
              for (int j=0;j<s2.length();j++) {//look for new <types> in this production
                c=s2.charAt(j);
                if (inStr) //when inside a string, keep searching for the end
                  inStr=(c!='\'');
                else if (c=='\'' && !inType)  //start a new string
                  inStr=true;
                else if (c=='<' && !inType) { //start a new type
                  currType.setLength(0);
                  inType=true;
                } else if (inType) { //when inside a type (<...>), ensure legal chars until done
                  if (c=='>') {//found a type
                    if (currType.length()>0) {//add type if it's new
                      inType=true; //gets set false if not a new type
                      for (int k=0;k<type.size();k++)
                        if (((String)type.elementAt(k)).equals(currType.toString())) {
                          inType=false;
                          break;
                        }
                      if (inType) //if it's new, add it
                        type.addElement(currType.toString());
                    }
                    inType=false;
                  } else if ((c<'a' || c>'z') &&
                             (c<'A' || c>'z') &&
                             (c!='.')) {//illegal character for a type
                    inType=false;
                    currType.setLength(0);
                  } else //another legal char inside the type
                    currType.append(c);
                }//end if (inType)
              } //end for j, looking for new types

            } //end if (obj instanceof Parsable)
          } catch (InstantiationException e) { //instantiated an abstract class
          } catch (IllegalAccessException e) { //instantiated a private class
          } catch (NoSuchMethodError      e) { //why is this raised sometimes?
          } catch (ClassNotFoundException e) { //raised by VRML .class files
          } catch (NoClassDefFoundError   e) { //raised by netscape/plugin/Plugin
          } catch (UnsatisfiedLinkError   e) { //raised by VRML .class files
          }
      }
      files=currDir.list(dirFilter);
      if (files != null) {
        quicksort(files,0,files.length-1);
        for (int i=0;i<files.length;i++) {
          checkFiles(pkg+files[i]+".",
                     new File(currDir,files[i]));
        }
      }
    }
  } //end checkFiles

  /** Print the credits */
  public void paint(Graphics g) {
    Logo.credits(g,5,5);
  }

  //quicksort an array of strings (array can have 0 elements, or be null)
  void quicksort(String[] str,int first,int last) {
    int low,high,medium;
    String key,temp; //sort into less than, equal, and greater than key
    if (first>=last || str==null)  //sorting 0 or 1 elements is easy
      return;
    key=str[(first+last)/2];
    low=first;
    medium=first;
    high=last;
    while (medium<=high) {
      while (str[high].compareTo(key)>0 && medium<=high)
        high--;
      if (medium>high)
        break;
      if (str[medium].compareTo(key)<0) {
        temp=str[low];
        str[low]=str[medium];
        str[medium]=temp;
        medium++;
        low++;
      } else if (str[medium].compareTo(key)>0) {
        temp=str[high];
        str[high]=str[medium];
        str[medium]=temp;
        high--;
      } else
        medium++;
    }
    quicksort(str,first,low-1);
    quicksort(str,high+1,last);
  }//end quicksort

  //print a given number of spaces (nothing for nonpositive numbers)
  private final void spaces(int num) {
    for (int i=0;i<num;i++)
      print(" ");
  }

  //print a newline
  private final void println() {
    System.out.println();
  }

  //print string then newline, replacing < and > with &lt and &gt if html
  private final void println(String s) {
    print(s);
    System.out.println();
  }

  //print string replacing < and > with &lt and &gt if html
  private final void print(String s) {
    char c;
    if (html) {
      for (int i=0;i<s.length();i++) {
        c=s.charAt(i);
        if      (c=='<') System.out.print("&lt");
        else if (c=='>') System.out.print("&gt");
        else             System.out.print(c);
      }
    } else
      System.out.println(s);
  }

  //get the BNF of an object, either through the parameters array or, if
  //there is none, through its BNF() function.
  private String getBNF(Parsable p,int lang) {
    Object[][] par=p.getParameters(lang);
    if (par==null)
      return p.BNF(lang);
    String s="'{' (";
    for (int i=0;i<par[1].length;i+=3) {
      Object obj;
      obj=par[1][i+1];
      if (i>0)
        s+=" | ";
      s+="('"+par[1][i]+"' ";
      //find the type for this parameter by trying to cast to various types
      try {
        PString temp1; //BUG: CAFE doesn't raise the exception with this line merged with next
        temp1=(PString)(obj);
        s+="<string>";
      } catch (ClassCastException e1) {
        try {
          PBoolean temp2;
          temp2=(PBoolean)obj;
          s+="<boolean>";
        } catch (ClassCastException e4) {
          try { //if it's Parsable, then parse class
            Parsable temp5;
            temp5=(Parsable)obj;
            s+=temp5.getClass().getName();
          } catch (ClassCastException e5) { //it's not a Parsable, so check if it's a Parsable[]
            try {
              Parsable[] temp6;
              temp6=(Parsable[])obj;
              String ss=temp6.getClass().getName();
              s+="<"+ss.substring(2,ss.length()-1)+">"; //Parsable[] represents a <type>
            } catch (ClassCastException e6) {
              try {
                Parsable[][] temp7;
                temp7=(Parsable[][])obj;
                String ss=temp7[0].getClass().getName();
                if (temp7.length==2)
                  s+="'{' "+ss.substring(2,ss.length()-1)+"+ '}'"; //Parsable[][] represents a class list
                else
                  s+="'{' <"+ss.substring(2,ss.length()-1)+">+ '}'"; //Parsable[][] represents a <type> list
              } catch (ClassCastException e7) {
                System.out.println("error in parameters array: parameter '"+par[1][i]+"' not a Parsable or Parsable[] or Parsable[][]");
                System.out.println("error="+e7);
                System.out.println("obj="+obj);
                throw e7;
              }
            }
          }
        }
      }
      s+=")";
    }
    s+=")* '}'//"+par[0][0]+". ";
    for (int i=0;i<par[1].length;i+=3)
      s+="\n  "+par[1][i]+": "+par[1][i+2];
    return s;
  }
} // end class BNF

//filter out all filenames but .class files
class ClassFilter implements FilenameFilter {
  public boolean accept(File dir,String name) {
    return name.endsWith(".class");
  }
} //end class ClassFilter

//filter out all but directories
class DirFilter implements FilenameFilter {
  public boolean accept(File dir,String name) {
    return !name.endsWith(".class");
  }
}//end class DirFilter
