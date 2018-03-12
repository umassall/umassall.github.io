package parse;
import java.net.URL;
import java.io.InputStream;

/** A Scanner breaks a text file up into tokens.  It is initialized with
  * a filename, a string representing the next token each time
  * get() is called, and closes the file when close() is called, or when
  * it is finalized.  The string returned by get() is empty
  * if there are no more tokens in the file, the file is empty, or the file
  * doesn't exist.  whitespace is ignored, as are comments which start with
  * // and go to the end of the line, or start and end with /* and * /
  * and can be nested.  A token starting with a digit,".","+",or "-" is treated
  * like a C number.  A token starting with a letter or "_" is treated like
  * a C identifier.  Everything else, such as "++" is broken up into
  * one-character tokens.  A // causes the rest of the line to be ignored,
  * including any /* or * / that might be on that line.  Comment symbols
  * within double-quoted strings are considered part of the string. An
  * end of line will also end a string, but not put the quote mark at
  * the end. a /* can also be terminated by the end of the file. Within
  * a comment block starting with /* the comment symbol // is not ignored.
  * A string starts with a quote and ends with either a quote or end of line.
  * Strings are returned without the enclosing quotes.
  * Identifiers are case sensitive and include "abc", "abc.def.ghi", "true".
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 28 March 96
  * @author Leemon Baird
  */
public class Scanner {
  protected final char CR='\r'; //carriage return character
  protected final char LF='\n'; //line feed character
  protected final char PCR='`'; //pseudo CR compensating for Netscape bug
  protected boolean streamEmpty=false; //return EOF tokens when nothing left to scan
  protected char     c=' ';  //next character to process, remembered from last get()
  protected char lastC=' ';  //last character processed before c
  protected InputStream infile=null; //the stream to read from
  protected int line=1;       //curr line of input file
  protected int pos =0;       //curr position on curr line of input file
  protected StringBuffer buf=null; //buffer holding current token as it's built
  protected int clev=0; //comment level: # /*...*/ pairs currently inside
  protected int s=0;    //finite state machine starts in 0, skipping initial whitespace
  protected int i=0;    //temporarily holds each character to check for end of file
  protected int languageMode=0; //0=C, 1=HTML, 2=LISP

  public Scanner(java.io.InputStream stream) {
    infile=stream;
  }
  /** close the file if it isn't already closed
    */
  public void close() {
    if (infile!=null)
      try {
        infile.close();
      } catch (java.io.IOException e) {} //ignore errors
  } //end method close

  /** finalize closes the file if it isn't already closed
    * @exception java.lang.Throwable an error happened during finalization.
    */
  public void finalize() throws java.lang.Throwable {
    close();
    super.finalize();
  } //end method finalize

  /** Set the scanner mode, 0=C, 1=HTML, 2=LISP. */
  public void setScannerMode(int mode) {
    languageMode=mode;
  }
  /** Get the scanner mode, 0=C, 1=HTML, 2=LISP. */
  public int getScannerMode() {
    return languageMode;
  }

  /** Get a token and place it in the appropriate variables in the parser. */
  public void get(Parser p) {
    buf = new StringBuffer();
    clev=0; //comment level: # /*...*/ pairs currently inside
    s=0;    //finite state machine starts in 0, skipping initial whitespace
    i=0;    //temporarily holds each character to check for end of file
    if (streamEmpty) {  //return End Of File tokens after nothing left
      p.isEOF=true;
      return;
    }
    while (s<98) { //finite state machine
      switch (languageMode) { //transition s to the next state based on i
        case 0: nextStateC(p);    break;
        case 1: nextStateHTML(p); break;
        case 2: nextStateLISP(p); break;
      }
      if (s<99) {//state 99=done, don't read.  98=done, read another byte.
        try {
          i=infile.read();  //read one character as an int
        } catch (java.io.IOException e) {
          i=-1; //treat all errors as an End Of File
        }
        if (i==-1) { //"End Of File" is a -1
          streamEmpty=true; //future calls to get() all return EOF token
          if (buf.length()==0) { //return EOF token if nothing found so far
            p.isEOF=true;
            return;
          }
          c=CR; //otherwise insert a CR into the stream to end current token
        }
        lastC=c;   //remember last character read
        c=(char)i;  //convert the character to a char if not EOF
        pos++;
        if (((c== CR)&&(lastC!=LF)&&(lastC!=PCR)) ||   //go to next line when observing
            ((c== LF)&&(lastC!=CR)&&(lastC!=PCR)) ||   //  a CR or LF or PCR or CR+LF
            ((c==PCR)&&(lastC!=CR)&&(lastC!= LF))) {   //  or CR+PCR or LF+PCR all in any order
          pos=-1;
          line++;
        } //end if new line
      } //end if s<99
    } //end while s<98  (FSM main loop)
  } //end method get


  /** Perform the state transition for scanning C++-like code
    * with nested comments, and dots allowed as part of identifiers.
    */
  protected void nextStateC(Parser p) {
    switch(s) {
      //State  0             skips initial whitespace
      //State  4             deals with identifiers
      //State  12            deals with strings
      //States 1,5,6,9,10,13 deal with comments
      //States 2,3,7,8,11    deal with numbers
      //States 98,99         return the token found (98=read another character, 99=don't)
      case 0: //skipping initial whitespace
              p.line=line;  //remember where token started
              p.pos =pos;
              if (c==' ' || c==LF || c==CR || c==PCR)  break;
              if (c=='/')              {s=1; break;}   //1=skipping whitespace, saw one slash
              if (c=='\"')             {s=12;break;}   //12=inside a "..." quote
              if (c=='\'')             {s=15;break;}   //15=inside a '...' quote
              if (c>='0' && c<='9')    {s=2; buf.append(c);break;} //2=integer part of a number
              if (c=='.')              {s=3; buf.append(c);break;} //3=fractional part of a number
  //            if (c=='-'||c=='+')      {s=11;buf.append(c);break;} //11=have seen only a single + or -
  //            deleted state 11 so -3.5 is interpreted as a minus sign then a 3.5
              if (c=='_' || c=='#' ||
                  (c>='a' && c<='z') ||
                  (c>='A' && c<='Z'))  {s=4;buf.append(c);break;} //4=identifier or boolean
              p.isChar=true;
              p.tChar=c;
              s=98; //98=return the token found then read another character
              break;
      case 1: //skipping whitespace, saw one slash
              if (c=='/') {s=5;break;}         //5=skipping rest of line after a //
              if (c=='*') {s=6; clev++;break;} //6=//inside a /*...*/ comment block
              p.isChar=true;
              p.tChar='/';
              s=99; //99=return the token found, but don't read another character
              break;
      case 2: //integer part of a number
              if (c>='0' && c<='9')      {buf.append(c);break;}
              if (c=='.')           {s=3; buf.append(c);break;} //3=fractional part of a number
              if (c=='e' || c=='E') {s=7; buf.append(c);break;} //7=start of exponent after the E or e.
              p.isInt=true;
              p.tInt=Integer.parseInt(buf.toString());
              s=99; //99=return the token found, but don't read another character
              break;
      case 3: //fractional part of a number
              if (c>='0' && c<='9')      {buf.append(c);break;}
              if (c=='e' || c=='E') {s=7; buf.append(c);break;} //7=start of exponent after the E or e.
              p.isDouble=true;
              p.tDouble=Double.valueOf(buf.toString()).floatValue();
              s=99; //99=return the token found, but don't read another character
              break;
      case 4: //identifier or boolean
              if (c=='_' ||
                  c=='.' ||
                  (c>='a' && c<='z') ||
                  (c>='A' && c<='Z') ||
                  (c>='0' && c<='9')) {buf.append(c);break;}
              if (buf.toString().equalsIgnoreCase("true"))
                p.isBoolean=p.tBoolean=true;
              else if (buf.toString().equalsIgnoreCase("false")) {
                p.isBoolean=true;
                p.tBoolean=false;
              } else {
                p.isID=true;
                p.tID=buf.toString();
              }
              s=99; //99=return the token found, but don't read another character
              break;
      case 5: //skipping rest of line after a //
              if (c==LF || c==CR || c==PCR)
                s=0; //0=skipping initial whitespace
              break;
      case 6: //inside a /*...*/ comment block
              if (c=='*') {s=9; break;} //9=a * seen inside a /*...*/ comment block
              if (c=='/') {s=10;break;} //10=a / seen inside a /*...*/ comment block
              break;
      case 7: //start of exponent after the E or e.
              if (c>='0' && c<='9') {s=8; buf.append(c);break;} //8=middle of exponent
              if (c=='+' || c=='-') {s=14;buf.append(c);break;} //14=just saw + or - in an exponent after the E or e
              p.isDouble=true; // interpret 1.2e just like 1.2e0
              p.tDouble=Double.valueOf(buf.toString()+'0').doubleValue();
              s=99; //99=return the token found, but don't read another character
              break;
      case 8: //middle of exponent
              if (c>='0' && c<='9') {buf.append(c);break;}
              p.isDouble=true;
              p.tDouble=Double.valueOf(buf.toString()).doubleValue();
              s=99; //99=return the token found, but don't read another character
              break;
      case 9: //a * seen inside a /*...*/ comment block
              if (c=='/') {clev--;
                           if (clev>0) {s=6;break;}  //0=skipping initial whitespace
                           else        {s=0;break;}} //6=inside a /*...*/ comment block
              if (c=='*') {s=9;break;} //9=a * seen inside a /*...*/ comment block
              s=6; //6=inside a /*...*/ comment block
              break;
      case 10: //a / seen inside a /*...*/ comment block
              if (c=='*') clev++;
              if (c=='/') {s=13;break;}  ////13=after a // inside a /*...*/ comment block
              s=6;  //6=inside a /*...*/ comment block
              break;
//      case 11: //have seen only a single + or -
//              if (c>='0' && c<='9')    {s=2;buf.append(c);break;}  //2=integer part of a number
//              if (c=='.')              {s=3;buf.append(c);break;}  //3=fractional part of a number
//              p.isChar=true;
//              p.tChar=buf.charAt(0);
//              s=99; //99=return the token found, but don't read another character
//              break;
      case 12: //inside a "..." quote
              if (c=='\"' || c==CR || c==LF || c==PCR) { //end of line ends string
                p.isString=true;
                p.tString=buf.toString();
                s=98; //98=return the token found then read another character
                break;
              }
              buf.append(c);
              break;
      case 13: //after a // inside a /*...*/ comment block
              if (c==CR || c==LF || c==PCR) {s=6;break;} //6=inside a /*...*/ comment block
              break;
      case 14: //just saw + or - in an exponent after the E or e
              if (c>='0' && c<='9') {s=8; buf.append(c);break;} //8=middle of exponent
              p.isDouble=true; // interpret 1.2e+ just like 1.2e+0
              p.tDouble=Double.valueOf(buf.toString()+'0').doubleValue();
              s=99; //99=return the token found, but don't read another character
              break;
      case 15: //inside a '...' quote
              if (c=='\'' || c==CR || c==LF || c==PCR) { //end of line ends string
                p.isString=true;
                p.tString=buf.toString();
                s=98; //98=return the token found then read another character
                break;
              }
              buf.append(c);
              break;
    } //end switch(s)
  } //end function nextStateC

  /** Perform the state transition for scanning LISP */
  protected void nextStateLISP(Parser p) {
  } //end function nextStateLISP

  /** Perform the state transition for scanning HTML */
  protected void nextStateHTML(Parser p) {
  } //end function nextStateHTML
} // end class Scanner
