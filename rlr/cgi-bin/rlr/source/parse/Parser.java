package parse;
import pointer.*;
import expression.*;
import java.applet.Applet;
import java.io.ByteArrayInputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;
import java.net.URL;

/** Parse a file/URL/string, and return the Object it describes.  As it
  * finds a token in the file that is an identifier, it will interpret
  * it as the name of a class, load and instantiate that class, and
  * call methods on that class to find out what parameters it expects
  * in the file.  In this way, simply placing a new .class file
  * (of type Parsable) in the directory will automatically extend the
  * language that Parser parses.  An object can be defined as part of
  * several different languages.  When parsing, and integer is passed
  * in to select which language.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 10 March 96
  * @author Leemon Baird
  */
public class Parser {
  public boolean   isChar,isString,isInt; //is current token this type?
  public boolean   isDouble,isID,isEOF;   //(ID=identifier, EOF=end of file)
  public boolean   isBoolean;
  public char      tChar;                 //one of these 6 variables
  public int       tInt;                  //  contains the current token
  public double    tDouble;
  public String    tString;
  public String    tID;
  public boolean   tBoolean;
  public int       line;                  //which line the current token started on
  public int       pos;                   //which position on the line the token started on
  public Applet    applet=null;           //the applet doing the parsing
  public Hashtable labels=new Hashtable();//given an obect find its #DEF/#USE name (used by the unparser)

  protected String  fname="InputStream";  //filename (or URL, etc.) printed when there's an error
  protected Scanner scanner=null;         //the source of the tokens being parsed
  protected boolean getTokenCalled;       //set true by getToken. Used by parseClassList
  protected Vector  symbolTable;          //a vector of hash tables (each is one scope)
  protected int     lastDir=0;            //last directory in which a new class was found

  /** create a new parser with a blank symbol table */
  public Parser() {
    super();
    symbolTable=new Vector();
    enterScope(); //create the table for global symbols to start with
  }

  /** create a new parser with a blank symbol table, and start parsing the string */
  public Parser(String str) {
    startParseString(str);
  }

  /** erase the current token variables, set booleans to false. */
  final void resetCurrToken() {
    isChar=isString=isInt=isDouble=isID=isEOF=isBoolean=false;
    tChar=' ';
    tDouble=0;
    tString=tID="";
    tBoolean=false;
    tInt=line=pos=0;
  }

  /** starts the parser at the beginning of the given string. Returns
    * true if successful, false if an error occurs.
    */
  public final boolean startParseString(String str) {
    byte byteSource[];
    if (str==null)
      return false; // can't parse an empty string
    byteSource=new byte[str.length()];
    if (str.length()<10)
      fname="string '"+str+"': ";
    else
      fname="string '"+str.substring(0,str.length()>55 ? 55 : str.length())+"...': ";
    for (int i=0;i<str.length();i++) //take first 8 bits of each character
      byteSource[i]=(byte)str.charAt(i);
    return startParseInputStream(new ByteArrayInputStream(byteSource));
  }

  /** starts the parser at the beginning of the given file. Returns
    * true if successful, false if the file doesn't exist or has a
    * security error.
    */
  public final boolean startParseFile(String filename) {
    if (filename==null)
      return false; // can't parse with no filename
    fname="File "+filename+": ";
    try {
      return startParseInputStream(new FileInputStream(filename));
    } catch (java.io.FileNotFoundException e) {
      return false;
    }
  }
  /** starts the parser at the beginning of the given URL. Returns
    * true if successful, false if the URL doesn't exist or has a
    * security error.
    */
  public final boolean startParseURL(String url) {
    if (url==null)
      return false; // can't parse with no URL
    fname="URL: "+url;
    try {
      return startParseInputStream((new URL(url)).openStream());
    } catch (java.io.IOException e) {
      return false;
    }
  }
  /** starts the parser at the beginning of the given InputStream. Returns
    * true if successful, false if the URL doesn't exist or has a
    * security error.
    */
  public final boolean startParseInputStream (InputStream stream) {
    if (scanner!=null)
      scanner.close();             //close source file before opening another
    scanner = new Scanner((InputStream)new BufferedInputStream(stream));
    getToken();                    //get the first token from the stream
    return true;
  } //end method startParseInputStream

  /** Close the file being parsed. */
  public void close() {
    if (scanner!=null)
      scanner.close();
  }

  /** Gets the next token from the stream.  It is stored in the Parser
    * object.  If there are no more tokens, it will set isEOF to true.
    */
  public void getToken() {
    getTokenCalled=true; //let parseClassList know a token was consumed
    resetCurrToken();    //erase the current token variables
    scanner.get(this);   //get first token, set variables in Parser
  }

  /** Consume next token if it is character c, else do nothing.
    * Throw an exception if throwException and it is not the character.
    * @exception parse.ParserException parser didn't find the required token
    */
  public boolean parseChar(char c, boolean throwException) throws ParserException {
    if (isChar && tChar==c) { //next token is the character, so consume it
      getToken();
      return true;
    }else
      if (throwException)    //if it's not right, then complain (if enabled)
        error("token '"+c+"'");
    return false;
  } //end method parseChar

  /** Consume next token if it is the identifier id, else do nothing.
    * Throw an exception if throwException and it is not the identifier id.
    * @exception parse.ParserException parser didn't find the required token
    */
  public boolean parseID(String id, boolean throwException) throws ParserException {
    if (isID && tID.equals(id)) { //next token is right, so consume it
      getToken();
      return true;
    }else
      if (throwException)    //if it's not right, then complain (if enabled)
        error("token '"+id+"'");
    return false;
  } //end method parseID

  /** If next token is a double, return it.
    * Throw an exception if throwException and it is not a double.
    * @exception parse.ParserException parser didn't find the required token
    */
  public double parseDouble___(boolean throwException) throws ParserException {
    double ret=tDouble; //number to return
    if (isDouble)
      getToken();
    else
      if (throwException)
        error("double");
    return ret;
  } //end method parseDouble

  /** If next token is a boolean, return it.
    * Throw an exception if throwException and it is not a boolean.
    * @exception parse.ParserException parser didn't find the required token
    */
  public boolean parseBoolean(boolean throwException) throws ParserException {
    boolean ret=tBoolean; //value to return
    if (isBoolean)
      getToken();
    else
      if (throwException)
        error("boolean");
    return ret;
  } //end method parseBoolean

  /** If next token is an int, return it.
    * Throw an exception if throwException and it is not an int.
    * @exception parse.ParserException parser didn't find the required token
    */
  public int parseInt___(boolean throwException) throws ParserException {
    int ret=tInt; //number to return
    if (isInt)
      getToken();
    else
      if (throwException)
        error("int");
    return ret;
  } //end method parseInt

  /** If next token is a String, return it.
    * Throw an exception if throwException and it is not a String.
    * @exception parse.ParserException parser didn't find the required token
    */
  public String parseString(boolean throwException) throws ParserException {
    String ret=tString; //number to return
    if (isString)
      getToken();
    else
      if (throwException)
        error("string");
    return ret;
  } //end method parseString

  /** Loads a Parsable class from disk, instantiates it,
    * calls its parse() method, and returns whatever parse() returns.
    * If there is an error and throwException=false, then what it returns
    * is undefined (so don't assume that it will return null).
    * @exception parse.ParserException parser didn't find the required token
    */
  public Parsable parseClass(String className,
                           int    lang,
                           boolean throwException) throws ParserException {
    Parsable obj=null;  //object of class className
    Parsable ret=null;  //what to return after parsing it

    if (parseID("#USE",false)) { //#USE myName
      Parsable object=getSymbol(tID);
      if (object==null && throwException)
        error("Existing object name in #USE statement");
      getToken(); //consume the name
      return object;
    } else if (parseID("#DEF",false)) { //#DEF myName { ... }
      String id=tID; //name being defined
      if (!isID)
        error("Identifier for #DEF");
      getToken();    //consume the name
      parseChar('{',true);
      Parsable object=(Parsable)parseClass(className,lang,throwException);
      parseChar('}',true);
      setSymbol(id,object);  //store in the symbol table for the current scope
      labels.put(object,id); //tell the unparser that this object had this name
      return object;
    }

    obj=(Parsable)instantiateClass(className);
    if (obj==null && throwException)
      error("class "+className);

    if (throwException)
      ret=parseObject(obj,lang); //parse all following tokens, return result
    else { //throw exception only if part of the class is parsed before error.
      int line=scanner.line;
      int pos =scanner.pos;
      try {
        ret=parseObject(obj,lang); //parse all following tokens, return result
      } catch (ParserException e) {
        if (line!=scanner.line || pos!=scanner.pos)
          throw e; //an optional class must either be all there or none
      }
    }

    if (ret==null && throwException)
      error("class "+className);
    return ret;
  } //parseClass

  /** A parsable class should call parseType during parsing if the next
    * token to be parsed is an identifier representing the name of the
    * class that will continue parsing from there.  That object must be
    * of the specified type, or it is illegal.  Illegal objects either
    * cause parseClass to return null or to throw an exception, depending
    * on whether throwException is false or true respectively. If a class
    * is called a.b.Name, then it can be found by asking for
    * name, Name, or a.b.Name  (all three work).  The actual class name must not
    * start with a lower case.
    * If there is an error and throwException=false, then what it returns
    * is undefined (so don't assume that it will return null).
    * @exception parse.ParserException parser didn't find the required token
    */
  public Parsable parseType(String    type,
                            int       lang,
                            boolean   throwException) throws ParserException {
    Parsable obj=null;  //object of class tID (defined by next token)

    if (parseID("#DEF",false)) { //#DEF myName { ... }
      String id=tID; //name being defined
      if (!isID)
        error("identifier for #DEF");
      getToken();    //consume the name
      parseChar('{',true);
      obj=(Parsable)parseType(type,lang,throwException);
      parseChar('}',true);
      setSymbol(id,obj); //store in the symbol table for the current scope
      labels.put(obj,id); //tell the unparser that this object had this name
      return obj;
    } else if (parseID("#USE",false)) { //#USE myName
      if (!isID)
        error("Identifier to #USE");
      obj=getSymbol(tID);
      if (obj==null)
        error("#USE "+tID+" used an identifier not #DEFed "+
              "before.  Valid identifier");
      getToken(); //consume the name
      try { //if it's the wrong type then forget it exists
        if (!isOfType(obj.getClass(),Class.forName(type)))
          error("type "+type);
      } catch (ClassNotFoundException e) {
      }
      return obj;
    }

    if (!isID)  //fail if the next token isn't even an identifier
      if (throwException)
        error("type "+type);
      else {
        return null;
      }

    obj=(Parsable)instantiateClass(tID);  //try to instantiate the object
    if (obj==null)
      if (throwException)
        error("type "+type);
      else {
        return null;
      }
    try { //if it's the wrong type then forget it exists
      if (!isOfType(obj.getClass(),Class.forName(type))) {
        obj=null;
      }
    } catch (ClassNotFoundException e) {
      obj=null;
    }
    if (obj==null)
      if (throwException)
        error("type "+type);
      else {
        return null;
      }

    getToken(); //consume the token representing this class name
    obj=parseObject(obj,lang); //parse all following tokens, return result
    if (obj==null && throwException)
      error("type "+type); //complain if new object couldn't parse
    return obj;
  } //end method parseType

  /** Parse objects of class clss as long as possible, returning a vector of results.
    * If throwException, then an empty list is considered an error
    * @exception parse.ParserException parser didn't find the required token
    */
  public Vector parseClassList(String clss,
                               int lang,
                               boolean throwException) throws ParserException {
    Vector vect = new Vector();  //growable array of Parsable objects
    Parsable obj;                  //temporarily hold a new parsable object
    boolean oldGetTokenCalled=getTokenCalled;  //make nested lists work right
    if (throwException) //get first object, complain if not available
      vect.addElement(parseClass(clss,lang,true));
    try {
      while (true) { //get a list of objects of the right type
        getTokenCalled=false; //if exception raised, see if anything was parsed
        if ((obj=parseClass(clss,lang,false))==null)
          break; //quit if next token is not correct class
        vect.addElement(obj);
      }
    } catch (ParserException e) {
      if (getTokenCalled) //if error ocurred halfway through a parse of clss
        throw e;          //   then complain, else we're done with the list
    }
    if (vect.size()==0 && throwException) //don't allow empty lists
      error("class '"+clss+"'");
    getTokenCalled=oldGetTokenCalled; //ensure nested lists OK
    return vect;
  } //end method parseClassList

  /** Parse objects of type type as long as possible, returning a vector of results.
    * If throwException, then an empty list is considered an error
    * @exception parse.ParserException parser didn't find the required token
    */
  public Vector parseTypeList(String  type,
                              int     lang,
                              boolean throwException) throws ParserException {
    Vector vect = new Vector();  //growable array of Parsable objects
    Parsable obj;                  //temporarily hold a new parsable object
    while (true) { //get a list of objects of the right type
      if ((obj=parseType(type,lang,false))==null)
        break; //quit if next token is not of correct type
      vect.addElement(obj);
    }
    if (vect.size()==0 && throwException) //don't allow empty lists
      error("type '"+type+"'");
    return vect;
  } //end method parseTypeList

  /** If an error occurs during parsing, call this to throw an exception
    * and abort the parse. The message will say the filename,
    * what was expected (which is passed in), and the line and
    * position of the error within that file.
    * @exception parse.ParserException parser didn't find the required token
    */
  public void error (String expected) throws ParserException {
    ParserException exception;

    close();     //close scanner when aborting the parse
    exception=new ParserException(
                     "In "+fname+"\n     "+expected+
                     " expected on line " +line+", position "+pos+","+
                     " not "+
                     (isChar    ? "'"+tChar   +"'." :
                      isInt     ? "'"+tInt    +"'." :
                      isDouble  ? "'"+tDouble +"'." :
                      isString  ? "'"+tString +"'." :
                      isID      ? "'"+tID     +"'." :
                      isBoolean ? "'"+tBoolean+"'." :
                      "End of File."));
    exception.remember(this); //store line/pos/token/etc
    throw exception;
  } //end method error

  /** Is the specified Class of the specified Type?
    * Checks whether the type is the same as the class, or a superclass of it,
    * or an interface of it, and so on up the superclass/interface hierarchy.
    */
  public static final boolean isOfType(Class clss, Class type) {
    Class interfaces[]; //all the interfaces this class implements
    if (clss==null) //if end of recursion without success
      return false;
    if (clss.equals(type)) //if "type" is this class exactly
      return true;
    interfaces=clss.getInterfaces(); //check all the interfaces
    for (int i=0;i<interfaces.length;i++)
      if (isOfType(interfaces[i],type))
        return true;
    return isOfType(clss.getSuperclass(),type); //check all superclasses
  } //end method isOfType

  /** Associate a symbol with an object in the current scope. */
  public void setSymbol(String sym,Object obj) {
    ((Hashtable)(symbolTable.lastElement())).put(sym,obj);
  }

  /** Return the object associated with a symbol.
    * Returns one from the innermost (most recent) scope if it is
    * multiply defined. Return null if it is not defined.
    * Multiple calls to this return pointers to the same object.
    */
  public Parsable getSymbol(String sym) {
    Parsable obj;
    for (int i=symbolTable.size()-1; i>=0; i--) {
      obj= (Parsable)((Hashtable)(symbolTable.elementAt(i))).get(sym);
      if (obj!=null)
        return obj; //find symbol in innermost (most recent) scope possible
    }
    return null; //symbol not found in any scope
  }
  /** create a new hash table for symbols in a new scope. */
  public void enterScope() {
    symbolTable.addElement(new Hashtable()); //add a table for this scope
  }
  /** Delete the most recent hash table when exiting a scope */
  public void exitScope() {
    symbolTable.removeElementAt(symbolTable.size()-1);
  }
  /** Set the scanner into a given mode such as C, HTML, or LISP tokens. */
  public void setScannerMode(int mode) {
    setScannerMode(mode);
  }
  /** Get the scanner's mode, such as 0=C, 1=HTML, 2=LISP. */
  public int getScannerMode() {
    return scanner.getScannerMode();
  }


  /** Instantiate a class given its name, with or without the first letter
    * capitalized, and with or without the package name.
    * The following finds what string, if any, should be prepended to
    * className to create a complete class name (including package name) of a
    * class that exists.  It will first check parsedClasses for the needed string,
    * then try the advice in packageHints, then as a last resort do an exhaustive
    * search of Directories.dirs[].  Returns null if it doesn't exist.
    */
  private Parsable instantiateClass(String className) {
    Parsable obj=null; //object of class className
    int i=0,ii;        //indices used to search all directories for class

    //if it has a dot in it, try it without any prefix
    //(since it looks like it has the package name included already)
    if (className.indexOf(".")>-1) {
      obj=(Parsable)fix.Util.new_(className);
      if (obj!=null) {
        return obj;
      }
    }

    //convert first letter of class to upper case (if not already)
    if (Character.isLowerCase(className.charAt(0))) {
      className=className.substring(0,1).toUpperCase()+
                className.substring(1);
    }


    //check if the package name has already been found
    String packageName=(String)Directories.parsedClasses.get(className);
    if (packageName!=null) { //the package for this class was already found before
      return (Parsable)fix.Util.new_(packageName+className);
    }


    //haven't seen this one before, try checking the hints
    packageName=(String)Directories.packageHints.get(className);
    if (packageName!=null)
        obj=(Parsable)fix.Util.new_(packageName+className);
    if (obj!=null) {
      Directories.parsedClasses.put(className,packageName); //remember that hint was right
      return obj;
    }

    System.out.println("Parser is looking for class "+className);

    //when all else fails, do an exhaustive search for the package name
    for (ii=0;ii<Directories.dirs.length;ii++) { //search all directories
      i=(ii+lastDir)%Directories.dirs.length;
      if ((obj=(Parsable)fix.Util.new_(Directories.dirs[i]+className))!=null) {
        Directories.parsedClasses.put(className,Directories.dirs[i]);
        lastDir=i; //start the next exhaustive search where this one succeeded
        System.out.println("     Parser found the class "+className);
        return obj;
      }
    }

    //just can't find it anywhere
    return null;
  } //end instantiateClass

  //Parse parameters for a Parsable object and return the object.
  //This works by either using the parameters array that the object returns, or by
  //calling its parse() method.  The parse() method is called only if the parameters
  //returned were null.
  ///**/ needs to be able to handle save/load parameters too, when lang<0
  ///**/ Unparse and FindBnf need to be fixed similarly
  private final Parsable parseObject(Parsable obj,int lang) throws ParserException {
    Object[][] par=obj.getParameters(lang);
    if (par==null) //no automatic parsing
      return (Parsable)obj.parse(this, lang); //parse all tokens for that object, return result
    else { //the object wants automatic parsing
      parseChar('{',true);
      int i=0;
      if (lang<0 && parseChar('{',false)) { //parse the variables for the saved state
        for (int j=0;j<par[2].length;j++) { //read in all variables, or stop at "}"
          if (parseChar('}',false))
            break;  //for upward compatability, don't complain if not all vars were saved
            parseParameter("SavedState variable",par[2][j],lang);
            parseChar(',',true); //all vars followed by comma, even the last one
        }
        parseChar('}',false);
      }//end parsing variables for saved state
      while (true) { //loop until all parameters have been parsed
        for (i=0;i<par[1].length;i+=3) { //check each of the normal parameters (not special save/load ones)
          if (parseID((String)(par[1][i]),false)) { //if this is one of the parameters to be parsed
            parseParameter((String)par[1][i],par[1][i+1],lang);
            break; //found the parameter, so quit this for loop and go around the while again
          }
        }
        if (i>=par[1].length) //the for made a complete loop without finding any, so quit the while
          break;
      }
      parseChar('}',true);
      return obj;
    }
  }//end parseObject

  //parse the parameter described by obj.
  private void parseParameter(String parName,Object obj,int lang) throws ParserException {
    try { //if it's Parsable, then parse class
      Parsable temp;
      temp=(Parsable)obj;
      if (temp!=null)
        temp.parse(this,lang);
      else {
        System.out.println("error: variable for parameter "+parName+" is null");
      }
    } catch (ClassCastException e) { //it's not a Parsable, so check if it's a Parsable[]
      try {
        Parsable[] x;
        x=((Parsable[])obj); //if it's a Parsable[], then do a parse type
        String s=x.getClass().getName();
        x[0]=parseType(s.substring(2,s.length()-1),lang,true);
      } catch (ClassCastException ee) {
        try {
          Parsable[][] x;
          x=((Parsable[][])obj); //if it's a Parsable[][], then do a parse type list
          String s=x[0].getClass().getName();
          parseChar('{',true);
          Vector v;
          if (x.length==2)
            v=parseClassList(s.substring(2,s.length()-1),lang,true);
          else
            v=parseTypeList(s.substring(2,s.length()-1),lang,true);
          parseChar('}',true);
          Parsable[] y=new Parsable[v.size()];
          x[0]=y;
          v.copyInto(x[0]);
        } catch (ClassCastException eee) {
          System.out.println("error in parameters array: parameter '"+parName+"' not a Parsable or Parsable[]");
          throw eee; //quit if an error in the parameters array in this object
        }
      }
    }
  }//end parseParameter
} //end class Parser
