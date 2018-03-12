package parse;

/** A parser can be created to read a text file.  When it finds an
  * an identifier, it treats it as a class of type Parsable,
  * instantiates it, and calls it to find what kinds of parameters
  * that class expects following its name.  In that way, an extendable
  * parser can be created such that the language it parses grows every
  * time a new .class object (of type Parsable) is placed in the directory.
  * If several languages are defined, then parsing, unparsing, and
  * BNF can be different for each language.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @see Parser
  * @version 1.1, 21 July 97
  * @author Leemon Baird
  */
public interface Parsable {
  //version 1.1, 21 July  97
  //version 1.0, 1  April 96
  /** The symbolic name for this object, or null if it's not in the symbol table.
    * This is filled in automatically by the parser and used by the unparser to
    * handle #DEF/#USE statements correctly.
    */
  public String symbol=null;

  /** Return a string representing the right side of the BNF definition
    * of what this class parses.  Use single quotes around terminals.
    * Use identifiers for nonterminals that are parsed by a specific class.
    * Use angled brackets <> around identifiers for nonterminals that are
    * parsed by any class of a specific type, where the first token in the
    * string being parsed will be the name of the class that parses the rest
    * of the string. Use parentheses to group, [] for zero or one copy
    * the * for 0 or more, and + for 1 or more.  Use | for OR, and
    * the dot (.) to start a short comment for this class.  The parameter
    * lang specifies the language for parsing and unparsing that the
    * BNF describes.  Finally, there should be a short, 4-5 word comment
    * starting with "//" and ending with "." followed by more detailed
    * comments explaining all the parameters, the defaults, and what
    * exactly the object does.
    */
  public String BNF(int lang);

  /** Return an object representing the results of parsing, starting
    * at the current token.  Return null in case of error.  The
    * parameter lang specifies the language to parse.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p, int lang) throws ParserException;

  /** Emit a series of strings that represents this object and its
    * current state, possibly calling the unparse() methods of child
    * objects in the process.  The file created in this way should
    * be readable by parse() to recreate the objects.
    * Lang is the language to unparse into (0 is the "normal" language).
    * If an object parses one language then unparses another, it
    * will act as a translator between the languages.
    * The cursor should end up on the same line as the last output
    * (i.e. end with an emit() not an emitLine()).
    */
  public void unparse(Unparser u, int lang);

  /** Return a parameter array if BNF(), parse(), and unparse() are to be automated, null otherwise.
    * If this method returns something other than null, then the BNF(), parse(), and unparse()
    * methods will never be called, so they can be very simple (doing nothing but returning null);
    * This automatic system works for any
    *                                                                                        <pre>
    * The array is of type Object[][], and should be of the form:
    *    {{String},              //a short comment, period, then longer comment for BNF documentation for this Class
    *     {String, var, String,  //the name of a parameter in the BNF, and the Pointer to hold the parsed value
    *      String, var, String,  //another parameter
    *      String, var, String}, //another parameter, etc. (zero or more pairs in all)
    *     {var,                  //a Pointer holding a value to parse and unparse only during "save-all" experiment saves
    *      var,                  //another such parameter
    *      var}}                 //another such parameter (zero or more in all)
    *                                                                                        </pre>
    * In the above description, a Pointer such as a PInt means that an integer expression will
    * be parsed and placed into the existing PInt.  The PInt itself should already exist before
    * returning this array.  At any point in the array where a Pointer is legal, it is also legal
    * to put a single-element Parsable[] array.  So, if Foo implements Parsable,
    * then it is legal to put a variable there that contains a Foo[1] array.  So, for example:
    *                                                                                        <pre>
    *   IntExp          numCopies=new IntExp(0);
    *   PictureDrawer[] pic={null};
    *   Object[][]      parameters=
    *     {{"Draw N copies of a picture. The PictureDrawer must be given."},
    *      {"N",numCopies,"",
    *       "picture",pic,""},
    *      {}};
    *                                                                                        </pre>
    * This works if IntExp is a Pointer and PictureDrawer is a Parsable.  After parsing,
    * the number of copies will be an integer stored in numCopies.val and the PictureDrawer
    * will be stored in pic[0].
    *
    * The kind of parsing that is done depends on how the variable is defined.
    * Assuming that MyClass extends Pointer, and MyType implements Parsable,
    * the four types of parameters can be declared this way:
    *
    *     MyClass      a=new MyPointer();       //will parse the parameters for class MyClass
    *     MyType[]     b={null};                //will parse the name of a class of type MyType, then its parameters
    *     Parsable[][] c={new MyType[0]};       //will parse a list of objects of type MyParsable, parsing the class name and parameters for each one
    *     Parsable[][] d={new MyClass[0],null}; //will parse a list of objects of class MyClass, parsing the parameters for each one
    *
    *     MyClass      e=new MyPointer();       //a variable that is saved but not parsed
    *     MyClass      f=new MyPointer();       //a variable that is saved but not parsed
    *
    *     Object[][]   parameters=
    *     {{"A short description.  The longer description of this class comes after the period"},
    *      {"par1", a, "comment on this parameter",
    *       "par2", b, "another comment",
    *       "par3", c, "comment",
    *       "par4", d, "comment"},
    *      {e,f}}    //list of all the variables to save, with no strings
    *
    */
  public Object[][] getParameters(int lang);

  /** Initialize, either partially or completely.  After all objects have been parsed,
    * then all Watcher objects have had setWatchManager called, all objects should then
    * have initialize(0) called.  Also, after an object is cloned, the new copy
    * might have initialize(0) called.  If the level is greater than 0, then
    * the initialization is less complete.  For example, in a reinforcement learning
    * experiment, the different levels are:              <pre>
    *   0 new object:     initialize everything, allocate new arrays
    *                     (this is only called once)
    *
    *   1 new experiment: initialize everything, don't reallocate arrays,
    *                     set the random number seed to some standard value
    *
    *   2 new run         initialize weights, but don't reset the seed,
    *                     don't forget results of previous runs
    *
    *   3 new trial       initialize state, but don't reset the seed or
    *                     weights or previous results                        </pre>
    */
  public void initialize(int level);
}
