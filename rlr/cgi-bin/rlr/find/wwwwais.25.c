/*
** wwwwais 2.5
** 11/2/94
** waisq/waissearch to HTML interface
** By Kevin Hughes, kevinh@eit.com
**
** 1.0 : Old waissearch interface code. Yuck!
** 2.0 : New waisq interface.
** 2.1 : Speed improvements and gettitle() fix. Access control, purified!
** 2.2 : Added in better waissearch code, compatibility with non-URL indices.
** 2.3 : Close stderr...
** 2.4 : sys/types.h, multiline titles, better lstrstr(), supports GET, POST,
**       and PATH_INFO, better filename gathering for waisq and waissearch,
**       environment variables, configuration file, title HTML, multiple
**       WAIS sources, configurable icons, source recognition, file retrieval,
**       signals.
** 2.41: Changed waisq flag, better waissource config file check, mystrdup(),
**       translates CR/LF input to spaces.
** 2.42: <string.h>, *duh*..., WWWW_SELECTION.
** 2.5 : SWISH support added, TIMEOUT value, realbytes deleted, negative
**       line info not printed, selection fix.
**
** Thanks to Andrew Williams, Christian Bartholdsson, Enzo Michelangeli,
** Achim Jung, Alexander Gagin, Jim Robb, and many others for good
** suggestions and patches.
*/

/* You will need to define the following option! */

#define CONFFILE "/usr/local/httpd/conf/wwwwais.conf"
	/* The configuration file for wwwwais that holds all the defaults.
	*/

/* End of user-definable options */

#include <sys/types.h>
#include <stdio.h>
#include <ctype.h>
#include <sys/stat.h>
#include <signal.h>
#include <string.h>

#define VERSION "2.5"
#define WAISTITLE "WAIS Gateway"
#define VERSTITLE "WAIS Gateway Version Information"
#define DOCURL "http://www.eit.com/software/wwwwais/wwwwais.html"
#define TIMEOUT 45
#define MAXARGS 100
#define MAXARGLEN 1000
#define MAXSTRLEN 1024
#define MAXTITLELEN 1000
#define HEXLEN 5
#define TITLETOPLINES 12
#define SELECTTXT "selection="
#define SOURCETXT "source="
#define SOURCEDIRTXT "sourcedir="
#define MAXHITSTXT "maxhits="
#define VERSIONTXT "version="
#define ISINDEXTXT "isindex="
#define KEYWORDSTXT "keywords="
#define SORTTYPETXT "sorttype="
#define PORTTXT "port="
#define HOSTTXT "host="
#define SEARCHPROGTXT "searchprog="
#define ICONURLTXT "iconurl="
#define USEICONSTXT "useicons="
#define DESCTXT "getdesc="
#define DOCNUMTXT "docnum="
#define NO_ICON "no icon"
#define NO_DESC "(No description)"
#define NO_SELECT "none"
#define ICONVAR "$ICONURL"
#define KEYWORDVAR "$KEYWORDS"
#define TI_OPEN 1
#define TI_CLOSE 2
#define TI_FOUND 4

struct entry {
	int score;
	int lines;
	int bytes;
	int docnum;
	char *filename;
	char *title;
	char *type;
	char *icon;
	struct entry *left;
	struct entry *right;
} *entrylist;

struct source {
        char *line;
        struct source *next;
} *sourcelist, *ruleslist, *desclist, *bodylist;

struct suffixentry {
	char *suffix;
	char *desc;
	char *url;
	char *mime;
	struct suffixentry *next;
} *suffixlist;

char *getvalue();
char *getkeywords();
char *mystrdup();
void *emalloc();
int getnumber();
char *getqfilename();
char *getsfilename();
char *gettype();
char *gettitle();
char *getmime();
char *parsetitle();
char *geticon();
char *lstrstr();
struct entry *addentry();
char *getconfvalue();
char *getdesc();
struct source *addsource();
char *encode();
char *decode();
struct suffixentry *addsuffix();
char *replace();
char *getword();
char *ruleparse();
char *getrule();
void badconnection();
void badsegviol();
void badtimeout();

int use_icons, indexsources,
	no_options, use_selection,
	docnum, version;
int skip[MAXSTRLEN];
static char query_string[MAXSTRLEN],
	sorttype[MAXSTRLEN],
	swishbin[MAXSTRLEN],
	waisqbin[MAXSTRLEN],
	waissearchbin[MAXSTRLEN],
	selfurl[MAXSTRLEN],
	iconurl[MAXSTRLEN],
	useicons[MAXSTRLEN],
	pagetitle[MAXSTRLEN],
	selection[MAXSTRLEN],
	addrmask[MAXSTRLEN],
	unknown_icon[MAXSTRLEN],
	unknown_type[MAXSTRLEN],
	unknown_mime[MAXSTRLEN],
	srcdesc[MAXSTRLEN],
	source[MAXSTRLEN],
	sourcedir[MAXSTRLEN],
	maxhits[MAXSTRLEN],
	host[MAXSTRLEN],
	port[MAXSTRLEN],
	searchprog[MAXSTRLEN],
	keywords[MAXSTRLEN];

main ()
{
	int i, len;
	char *p, *method, *query, *addr;

	fclose(stderr);

	sorttype[0] = '\0';
	selection[0] = '\0';
	srcdesc[0] = '\0';
	sourcelist = NULL;
	suffixlist = NULL;
	ruleslist = NULL;
	desclist = NULL;
	bodylist = NULL;
	use_icons = 0;
	use_selection = 0;
	indexsources = 0;
	docnum = -1;

	signal(SIGSEGV, badsegviol);
	signal(SIGPIPE, badconnection);
	signal(SIGALRM, badtimeout);

/* First, get all the normal environment CGI things...
*/

	method = (char *) getenv("REQUEST_METHOD");
	if (method == NULL)
		progerr("Unknown method.");
	if (!strncmp(method, "POST", 4)) {
		len = atoi(getenv("CONTENT_LENGTH"));
		i = 0;
		while (len-- && i < MAXSTRLEN)
			query_string[i++] = fgetc(stdin);
		query_string[i] = '\0';
	}
	else if (!strncmp(method, "GET", 3)) {
		query = (char *) getenv("QUERY_STRING");
		if (query == NULL)
			query_string[0] = '\0';
		else
			strcpy(query_string, query);
	}
	else
		progerr("Unknown method.");

/* Does the user want version information?
** Is a document number associated with the request?
** Will the request be a WAIS source description?
*/

	version = ((getvalue(VERSIONTXT, ""))[0] == '\0') ? 0 : 1;
	docnum = atoi(getvalue(DOCNUMTXT, "-1"));
	strcpy(srcdesc, (char *) getvalue(DESCTXT, srcdesc));

/* Read the config file and determine any selected pop-up option...
*/

	strcpy(selection, (char *) getvalue(SELECTTXT, selection));
	if ((p = (char *) getenv("WWWW_SELECTION")) != NULL)
		strcpy(selection, p);
	if (selection[0] == '\0' || selection[0] == NULL)
		use_selection = 0;
	if (!strcmp(selection, NO_SELECT))
		no_options = 1;
	else
		no_options = 0;
	getdefaults();

/* Make sure the user's address is OK...
*/

	addr = (char *) getenv("REMOTE_ADDR");
	if (addr == NULL || !isokstring(addr, addrmask)) {
		printreject();
		exit(0);
	}

/* Grab all the normal variables.
*/

	strcpy(maxhits, (char *) getvalue(MAXHITSTXT, maxhits));
	if ((p = (char *) getenv("WWWW_MAXHITS")) != NULL)
		strcpy(maxhits, p);
	strcpy(sorttype, (char *) getvalue(SORTTYPETXT, sorttype));
	if ((p = (char *) getenv("WWWW_SORTTYPE")) != NULL)
		strcpy(sorttype, p);
	strcpy(iconurl, (char *) getvalue(ICONURLTXT, iconurl));
	if ((p = (char *) getenv("WWWW_ICONURL")) != NULL)
		strcpy(iconurl, p);
	strcpy(useicons, (char *) getvalue(USEICONSTXT, useicons));
	if ((p = (char *) getenv("WWWW_USEICONS")) != NULL)
		strcpy(useicons, p);
	strcpy(keywords, (char *) getkeywords());
	if (lstrstr(keywords, KEYWORDSTXT))
		strcpy(keywords, "");
	if ((p = (char *) getenv("WWWW_KEYWORDS")) != NULL)
		strcpy(keywords, p);

/* If there is a pop-up selection, these variables (host, port, etc.)
** are overridden.
*/

	if (!use_selection) {
		strcpy(source, (char *) getvalue(SOURCETXT, source));
		if ((p = (char *) getenv("WWWW_SOURCE")) != NULL)
			strcpy(source, p);
		strcpy(sourcedir, (char *) getvalue(SOURCEDIRTXT, sourcedir));
		if ((p = (char *) getenv("WWWW_SOURCEDIR")) != NULL)
			strcpy(sourcedir, p);
		strcpy(host, (char *) getvalue(HOSTTXT, host));
		if ((p = (char *) getenv("WWWW_HOST")) != NULL)
			strcpy(host, p);
		strcpy(port, (char *) getvalue(PORTTXT, port));
		if ((p = (char *) getenv("WWWW_PORT")) != NULL)
			strcpy(port, p);
		strcpy(searchprog, (char *) getvalue(SEARCHPROGTXT,
		searchprog));
		if ((p = (char *) getenv("WWWW_SEARCHPROG")) != NULL)
			strcpy(searchprog, p);
	}

	entrylist = NULL;

	if (lstrstr(useicons, "yes"))
		use_icons = 1;
	else
		use_icons = 0;

/* Do any searching that needs to be done.
** If there are no keywords, just print a form.
*/

	if (srcdesc[0] != '\0') {
		alarm(TIMEOUT);
		descwaiss();
	}
	else if (!keywords[0] && !version) {
		printheader();
		printform();
		return 0;
	}
	else {
		if (version) {
			printversion();
			if (isfile(waisqbin))
				dowaisq();
			if (isfile(waissearchbin))
				dowaiss();
			if (isfile(swishbin))
				doswish();
		}
		else {
			alarm(TIMEOUT);
			if (!strcmp(searchprog, "waisq"))
				dowaisq();
			else if (!strcmp(searchprog, "waissearch"))
				dowaiss();
			else
				doswish();
			printfooter();
		}
	}

	return 0;
}

/* This gets a value for a certain variable from the environment,
** using PATH_INFO and POST and GET information. It returns a
** default if the variable is not found.
*/

char *getvalue(var, def)
     char *var;
     char *def;
{
	int i;
	char *c, *argp, argstr[MAXSTRLEN];
	static char value[MAXSTRLEN], tmpstr[MAXSTRLEN];

	if (query_string[0] == '\0' || !lstrstr(query_string, var)) {
		argp = (char *) getenv("PATH_INFO");
		if (argp == NULL || !lstrstr(argp, var)) {
			if (strlen(def) <= 1)
				return "\0";
			strcpy(tmpstr, decode(def));
			return tmpstr;
		}
		strcpy(argstr, argp);
	}
	else
		strcpy(argstr, query_string);

	for (i = 0, c = (char *) lstrstr(argstr, var) +
	strlen(var); *c && i < MAXSTRLEN && *c != '&'; c++)
		value[i++] = *c;
	value[i] = '\0';

	if (i) {
		strcpy(tmpstr, decode(value));
		return tmpstr;
	}

	if (strlen(def) <= 1)
		return "\0";
	strcpy(tmpstr, decode(def));
	return tmpstr;
}

/* Search for keywords, either normally or through ISINDEX.
** If there are only keywords in environment information, they are returned.
*/

char *getkeywords()
{
	static char value[MAXSTRLEN], tmpvalue[MAXSTRLEN];

	strcpy(value, getvalue(ISINDEXTXT, ""));
	if (!value[0]) {
		strcpy(value, getvalue(KEYWORDSTXT, ""));
		if (!value[0]) {
			if (!strchr(query_string, '&') &&
			!strchr(query_string, '=')) {
				strcpy(tmpvalue, decode(query_string));
				return tmpvalue;
			}
			else
				return "\0";
		}
	}
	strcpy(tmpvalue, decode(value));

	return tmpvalue;
}

/* Prints the header file or string.
*/

printheader()
{
	char line[MAXSTRLEN];
	FILE *fp;

        printf("Content-type: text/html\n\n");

	if (lstrstr(pagetitle, ".html")) {
		if ((fp = fopen(pagetitle, "r")) != NULL) {
			while (fgets(line, MAXSTRLEN, fp) != NULL)
				printf("%s", line);
			fclose(fp);
		}
		else {
			printf("<title>%s</title>\n", WAISTITLE);
			printf("<h1>%s</h1>\n", WAISTITLE);
		}
	}
	else {
		printf("<title>%s</title>\n", pagetitle);
		printf("<h1>%s</h1>\n", pagetitle);
	}
}

/* Prints a link to the documentation.
*/

printfooter()
{
	printf("<hr>\n");
	printf("<i>This search was performed by <a href=\"%s\">wwwwais",
	DOCURL);
	printf(" %s</a>.</i>\n", VERSION);
}

/* The header for the version information page.
*/

printversion()
{
        printf("Content-type: text/html\n\n");
	printf("<title>%s</title>\n", VERSTITLE);
	printf("<h1>%s</h1>\n", VERSTITLE);
	printf("This is WWWWAIS version %s.\n<br>\n", VERSION);
}

/* Sets everything up for calling waisq and executes it.
** Spits out version information if requested.
*/

dowaisq()
{
	int i, j, k;
	char *argline[MAXARGS], word[MAXSTRLEN];
	int pipe0[2], pipe1[2];
	FILE *fp0;

	argline[0] = waisqbin;

	if (version) {
		argline[1] = (char *) mystrdup("-V");
        	argline[2] = (char *) 0;
	}
	else {
		argline[1] = (char *) mystrdup("-s");
		argline[2] = sourcedir;
		argline[3] = (char *) mystrdup("-f");
		argline[4] = (char *) mystrdup("-");
		argline[5] = (char *) mystrdup("-S");
		argline[6] = source;
		argline[7] = (char *) mystrdup("-m");
		argline[8] = maxhits;
		argline[9] = (char *) mystrdup("-g");

		for (i = 0, k = 10; k < MAXARGS;) {
			for (j = 0; keywords[i] != '\0' &&
			keywords[i] != ' ' && j < MAXSTRLEN; i++)
				word[j++] = keywords[i];
			word[j] = '\0';

			argline[k++] = (char *) mystrdup(word);

			if (keywords[i] == '\0')
				break;
			else if (keywords[i] == ' ')
				i++;
		}

		argline[k] = (char *) 0;
	}

	pipe(pipe0);
	pipe(pipe1);
	switch(fork()) {
	case -1:
		progerr("fork() call failed.");
	case 0:
		close(pipe0[1]);
		close(pipe1[0]);
		dup2(pipe0[0], 0);
		dup2(pipe1[1], 1);
		execv(waisqbin, argline);
		progerr("execv() call failed.");
	default:
		close(pipe0[0]);
		close(pipe1[1]);
		fp0 = fdopen(pipe1[0], "r");
		read_from_waisq(fp0);
		if (!version) {
			printheader();
			printform();

			if (entrylist == NULL) {
				notfound();
				return;
			}

			printf("Here is the result of your search using ");
			printf("the keyword(s) ");
			printf("<b>\"%s\"</b>:<p>\n", keywords);

			printf("<dl>\n");
			printentries(entrylist);
			printf("</dl>\n");
		}
	}
}

/* Sets everything up for calling waissearch and executes it.
** Spits out version information if requested.
*/

dowaiss()
{
	int i, j, k;
	char *argline[MAXARGS], word[MAXSTRLEN];
	int pipe0[2], pipe1[2];
	FILE *fp0, *fp1;

	argline[0] = waissearchbin;

	if (version) {
		argline[1] = (char *) mystrdup("-v");
        	argline[2] = (char *) 0;
	}
	else {
		argline[1] = (char *) mystrdup("-h");
		argline[2] = host;
		argline[3] = (char *) mystrdup("-p");
		argline[4] = port;
		argline[5] = (char *) mystrdup("-d");
		argline[6] = source;
		argline[7] = (char *) mystrdup("-m");
		argline[8] = maxhits;

		for (i = 0, k = 9;;) {
			for (j = 0; keywords[i] != '\0' &&
			keywords[i] != ' ' && j < MAXSTRLEN; i++)
				word[j++] = keywords[i];
			word[j] = '\0';

			argline[k++] = (char *) mystrdup(word);

			if (keywords[i] == '\0')
				break;
			else if (keywords[i] == ' ')
				i++;
		}

		argline[k] = (char *) 0;
	}

	pipe(pipe0);
	pipe(pipe1);
	switch(fork()) {
	case -1:
		progerr("fork() call failed.");
	case 0:
		close(pipe0[1]);
		close(pipe1[0]);
		dup2(pipe0[0], 0);
		dup2(pipe1[1], 1);
		execv(waissearchbin, argline);
		progerr("execv() call failed.");
	default:
		close(pipe0[0]);
		close(pipe1[1]);
		fp0 = fdopen(pipe1[0], "r");
		fp1 = fdopen(pipe0[1], "w");
		read_from_waissearch(fp0);
		if (!version) {
			fprintf(fp1, "q\n");
			fflush(fp1);
			fprintf(fp1, "q\n");
			fflush(fp1);

			printheader();
			printform();

			if (entrylist == NULL) {
				notfound();
				return;
			}

			printf("Here is the result of your search using ");
			printf("the keyword(s) ");
			printf("<b>\"%s\"</b>:<p>\n", keywords);

			printf("<dl>\n");
			printentries(entrylist);
			printf("</dl>\n");
		}
	}
}

/* Sets everything up for calling waissearch and executes it.
** Here, waissearch is expected to return a source description or
** file.
*/

descwaiss()
{
	int i, j, k;
	char *argline[MAXARGS], word[MAXSTRLEN];
	int pipe0[2], pipe1[2];
	FILE *fp0, *fp1;

	argline[0] = waissearchbin;

	argline[1] = (char *) mystrdup("-h");
	argline[2] = host;
	argline[3] = (char *) mystrdup("-p");
	argline[4] = port;
	argline[5] = (char *) mystrdup("-d");
	argline[6] = source;
	argline[7] = (char *) mystrdup("-m");
	argline[8] = maxhits;

	for (i = 0, k = 9;;) {
		for (j = 0; keywords[i] != '\0' &&
		keywords[i] != ' ' && j < MAXSTRLEN; i++)
			word[j++] = keywords[i];
		word[j] = '\0';

		argline[k++] = (char *) mystrdup(word);

		if (keywords[i] == '\0')
			break;
		else if (keywords[i] == ' ')
			i++;
	}
	argline[k] = (char *) 0;

	pipe(pipe0);
	pipe(pipe1);
	switch(fork()) {
	case -1:
		progerr("fork() call failed.");
	case 0:
		close(pipe0[1]);
		close(pipe1[0]);
		dup2(pipe0[0], 0);
		dup2(pipe1[1], 1);
		execv(waissearchbin, argline);
		progerr("execv() call failed.");
	default:
		close(pipe0[0]);
		close(pipe1[1]);
		fp0 = fdopen(pipe1[0], "r");
		fp1 = fdopen(pipe0[1], "w");
		desc_waissearch(fp0, fp1);
	}
}

/* Sets everything up for calling swish and executes it.
** Spits out version information if requested.
*/

doswish()
{
	int i, j, k;
	char *argline[MAXARGS], word[MAXSTRLEN], sourcepath[MAXSTRLEN];
	int pipe0[2], pipe1[2];
	FILE *fp0;

	argline[0] = swishbin;

	if (version) {
		argline[1] = (char *) mystrdup("-V");
        	argline[2] = (char *) 0;
	}
	else {
		sprintf(sourcepath, "%s%s%s", sourcedir,
		(sourcedir[strlen(sourcedir) - 1] == '/') ?
		"" : "/", source);
		argline[1] = (char *) mystrdup("-f");
		argline[2] = sourcepath;
		argline[3] = (char *) mystrdup("-m");
		argline[4] = maxhits;
		argline[5] = (char *) mystrdup("-w");

		for (i = 0, k = 6; k < MAXARGS;) {
			for (j = 0; keywords[i] != '\0' &&
			keywords[i] != ' ' && j < MAXSTRLEN; i++)
				word[j++] = keywords[i];
			word[j] = '\0';

			argline[k++] = (char *) mystrdup(word);

			if (keywords[i] == '\0')
				break;
			else if (keywords[i] == ' ')
				i++;
		}

		argline[k] = (char *) 0;
	}

	pipe(pipe0);
	pipe(pipe1);
	switch(fork()) {
	case -1:
		progerr("fork() call failed.");
	case 0:
		close(pipe0[1]);
		close(pipe1[0]);
		dup2(pipe0[0], 0);
		dup2(pipe1[1], 1);
		execv(swishbin, argline);
		progerr("execv() call failed.");
	default:
		close(pipe0[0]);
		close(pipe1[1]);
		fp0 = fdopen(pipe1[0], "r");
		read_from_swish(fp0);
		if (!version) {
			printheader();
			printform();

			if (entrylist == NULL) {
				notfound();
				return;
			}

			printf("Here is the result of your search using ");
			printf("the keyword(s) ");
			printf("<b>\"%s\"</b>:<p>\n", keywords);

			printf("<dl>\n");
			printentries(entrylist);
			printf("</dl>\n");
		}
	}
}


/* Malloc's a string; returns it.
*/

char *mystrdup(s)
     char *s;
{
        char *p;

        p = (char *) emalloc(strlen(s) + 1);
        strcpy(p, s);
        return p;
}

/* Makes sure you have enough memory to continue...
*/

void *emalloc(i)
     int i;
{
        void *p;
 
        if ((p = (void *) malloc(i)) == NULL)
                progerr("Ran out of memory.");
        return p;
}

/* Whoops, caught a problem!
*/

progerr(errstring)
     char *errstring;
{
        printf("Content-type: text/html\n\n");
	printf("<title>WWWWAIS Error</title>\n");
	printf("<h1>WWWWAIS Error</h1>\n");
	printf("This program encountered an error:<p>\n");
	printf("<code>%s</code>\n<p>\n", errstring);
	exit(0);
}

/* Reads returned lines from waisq, grabs the relevant information,
** and sticks it all in the return information structure.
*/

read_from_waisq(fp)
     FILE *fp;
{
	int score, lines, bytes, skipline;
	static char buffer[MAXSTRLEN], filename[MAXSTRLEN], title[MAXSTRLEN],
	type[MAXSTRLEN], icon[MAXSTRLEN];

	skipline = 0;
	while (fgets(buffer, MAXSTRLEN, fp) != NULL) {
		if (strstr(buffer, "Information on database") ||
		strstr(buffer, "Catalog for database"))
			skipline = 1;
		if (version) {
			printf("%s\n", buffer);
			break;
		}
		if (strstr(buffer, "Search produced no"))
			return;
		if (strstr(buffer, ":score") && !skipline)
			score = getnumber(buffer, 1);
		if (strstr(buffer, ":original-local-id") && !skipline) {
			strcpy(filename, (char *) getqfilename(buffer));
			strcpy(type, (char *) gettype(filename));
			if (use_icons)
				strcpy(icon, (char *) geticon(filename));
			else
				strcpy(icon, NO_ICON);
			strcpy(title, (char *) gettitle(filename, -1));
		}
		if (strstr(buffer, ":number-of-lines") && !skipline)
			lines = getnumber(buffer, 1);
		if (strstr(buffer, ":number-of-bytes")) {
			if (skipline) {
				skipline = 0;
				continue;
			}
			bytes = getnumber(buffer, 1);
			entrylist = (struct entry *) addentry(entrylist,
			score, lines, bytes, filename, title, type, icon, -1);
		}
	}
}

/* Reads returned lines from waissearch, grabs the relevant information,
** and sticks it all in the return information structure.
*/

read_from_waissearch(fp)
     FILE *fp;
{
	int i, sdocnum, score, lines, bytes, found;
	static char buffer[MAXSTRLEN], filename[MAXSTRLEN], title[MAXSTRLEN],
	type[MAXSTRLEN], icon[MAXSTRLEN];

	found = -1;
        while (fgets(buffer, MAXSTRLEN, fp) != NULL) {
		if (found == 0)
			return;
		if (version) {
			printf("%s\n", buffer);
			break;
		}
		if (strstr(buffer, "NumberOfRecordsReturned")) {
			for (i = 0; buffer[i] != ':'; i++)
				;
			for (found = 0; buffer[i]; i++)
				if (isdigit(buffer[i]))
					found = (found * 10) +
					(buffer[i] - '0');
			if (!found)
				return;
		}
		if (strstr(buffer, "Information on database") ||
		strstr(buffer, "Catalog for database") ||
                strstr(buffer, "Search produced no") ||
		strstr(buffer, "is not available"))
                        return;
                if (strstr(buffer, "View document") ||
                strstr(buffer, "q to quit"))
                        return;
                if (strstr(buffer, "Score:")) {
			found--;
			for (i = sdocnum = 0; buffer[i] != ':'; i++)
				if (isdigit(buffer[i]))
					sdocnum = (sdocnum * 10) +
					(buffer[i] - '0');
			score = getnumber(buffer, 2);
			lines = getnumber(buffer, 3);
			strcpy(filename, (char *) getsfilename(buffer,
			sdocnum));
			bytes = getsize(filename);
			strcpy(type, (char *) gettype(filename));
			if (use_icons)
				strcpy(icon, (char *) geticon(filename));
			else
				strcpy(icon, NO_ICON);
			strcpy(title, (char *) gettitle(filename, sdocnum));
			entrylist = (struct entry *) addentry(entrylist,
			score, lines, bytes, filename, title, type, icon,
			sdocnum);
                }
        }
}

/* Reads lines returned from waissearch and grabs a source
** description or a file.
*/

desc_waissearch(fp, fp2)
     FILE *fp;
     FILE *fp2;
{
	int i, end, srcline, found, isdesc, gotmatch;
	char *c, *d, buffer[MAXSTRLEN], filename[MAXSTRLEN],
	maintainer[MAXSTRLEN], cost[MAXSTRLEN], costunit[MAXSTRLEN];

	found = -1;
	gotmatch = 0;
        while (fgets(buffer, MAXSTRLEN, fp) != NULL) {
		if (gotmatch && !strchr(buffer, ':'))
			break;
		if (strstr(buffer, "NumberOfRecordsReturned")) {
			found = getnumber(buffer, 1);
			if (!found)
				return;
		}
		if (strstr(buffer, "Information on database") ||
		strstr(buffer, "Catalog for database") ||
                strstr(buffer, "Search produced no") ||
		strstr(buffer, "is not available"))
                        return;
		if ((c = (char *) strstr(buffer, "lines:")) != NULL &&
		!gotmatch) {
			for (i = srcline = 0; buffer[i] != ':'; i++)
				if (isdigit(buffer[i]))
					srcline = (srcline * 10) +
					(buffer[i] - '0');
			strcpy(filename, getsfilename(buffer, srcline));
			if (docnum == srcline)
				gotmatch = 1;
		}
	}
	fprintf(fp2, "%d\n", srcline);
	fflush(fp2);
	fgets(buffer, MAXSTRLEN, fp);
	if (lstrstr(filename, ".src")) {
		isdesc = 1;
		end = 0;
        	while (fgets(buffer, MAXSTRLEN, fp) != NULL && !end) {
			if ((c = (char *) strstr(buffer, ":ip-name"))
			!= NULL) {
				c += strlen(":ip-name");
				strcpy(host, getword(c, &i));
			}
			else if ((c = (char *) strstr(buffer, ":cost "))
			!= NULL) {
				c += strlen(":cost ");
				strcpy(cost, getword(c, &i));
			}
			else if ((c = (char *) strstr(buffer, ":cost-unit :"))
			!= NULL) {
				c += strlen(":cost-unit :");
				strcpy(costunit, getword(c, &i));
			}
			else if ((c = (char *) strstr(buffer, ":tcp-port"))
			!= NULL) {
				c += strlen(":tcp-port");
				strcpy(port, getword(c, &i));
			}
			else if ((c = (char *) strstr(buffer, ":database-name"))
			!= NULL) {
				c += strlen(":database-name");
				strcpy(source, getword(c, &i));
			}
			else if ((c = (char *) strstr(buffer, ":maintainer"))
			!= NULL ) {
				c += strlen(":maintainer");
				strcpy(maintainer, getword(c, &i));
			}
			else if ((c = (char *) strstr(buffer, ":description"))
			!= NULL) {
				strcpy(buffer, replace(buffer, "\\\"", "\'"));
				c = (char *) strchr(buffer, '\"');
				while (c == NULL) {
					fgets(buffer, MAXSTRLEN, fp);
					c = (char *) strchr(buffer, '\"');
				}
				c++;
				if (numchar(buffer, '\"') == 2 &&
				(d = (char *) strrchr(buffer, '\"'))) {
					*d = '\0';
					desclist = (struct source *)
					addsource(desclist, c);
					break;
				}
				desclist = (struct source *)
				addsource(desclist, c);
       		 		while (fgets(buffer, MAXSTRLEN, fp) != NULL) {
					strcpy(buffer, replace(buffer, "\\\"",
					"\'"));
					if (c = (char *) strchr(buffer, '\"')) {
						*c = '\0';
						desclist = (struct source *)
						addsource(desclist, buffer);
						end = 1;
						break;
					}
					desclist = (struct source *)
					addsource(desclist, buffer);
				}
			}
		}
        }
	else {
		isdesc = 0;
        	while (fgets(buffer, 40, fp) != NULL) {
			if (!strncmp(buffer, "View doc", 8))
				break;
			bodylist = (struct source *)
			addsource(bodylist, buffer);
		}
	}
	fprintf(fp2, "q\n");
	fflush(fp2);
	fprintf(fp2, "q\n");
	fflush(fp2);

	indexsources = 0;
	if (isdesc) {
		no_options = 1;
		printheader();
		printform();
		printf("<h2>Database name: %s</h2>\n<p>\n", source);
		printf("<b>Maintainer:</b> <a href=\"mailto:%s\">%s</a>\n<p>\n",
		maintainer, maintainer);
		printf("<b>Description:</b>\n<p>\n");
		printf("<pre>\n");
		while (desclist != NULL) {
			printf("%s", desclist->line);
			desclist = desclist->next;
		}
		printf("</pre>\n<p>\n");
		printf("<b>Cost:</b> %s<br>\n", cost);
		printf("<b>Cost-unit:</b> %s<br>\n", costunit);
		printf("<b>Host:</b> %s<br>\n", host);
		printf("<b>Port:</b> %s<p>\n", port);
		printfooter();
	}
	else {
	        printf("Content-type: %s\n\n", getmime(filename));
		while (bodylist != NULL) {
			printf("%s", bodylist->line);
			bodylist = bodylist->next;
		}
	}
}

/* Reads returned lines from swish, grabs the relevant information,
** and sticks it all in the return information structure.
*/

read_from_swish(fp)
     FILE *fp;
{
	int i, j, score, bytes;
	static char buffer[MAXSTRLEN], filename[MAXSTRLEN], title[MAXSTRLEN],
	type[MAXSTRLEN], icon[MAXSTRLEN];

	while (fgets(buffer, MAXSTRLEN, fp) != NULL) {
		if (version) {
			printf("%s version: %s\n", swishbin, buffer);
			break;
		}
		if (buffer[0] == '.' || buffer[0] == 'e')
			break;
		if (buffer[0] == '#' || buffer[0] == 's')
			continue;
		if (strstr(buffer, "no results"))
			return;

		for (i = score = 0; isdigit(buffer[i]); i++)
			score = (score * 10) + (buffer[i] - '0');
		for (i++, j = 0; buffer[i] != ' '; i++)
			filename[j++] = buffer[i];
		filename[j] = '\0';
		for (i += 2, j = 0; buffer[i] != '\"'; i++)
			title[j++] = buffer[i];
		title[j] = '\0';
		for (i += 2, bytes = 0; isdigit(buffer[i]); i++)
			bytes = (bytes * 10) + (buffer[i] - '0');

		strcpy(type, (char *) gettype(filename));
		if (use_icons)
			strcpy(icon, (char *) geticon(filename));
		else
			strcpy(icon, NO_ICON);

		entrylist = (struct entry *) addentry(entrylist,
		score, -1, bytes, filename, title, type, icon, -1);
	}
}

/* Prints the generic form for searching.
*/

printform()
{
	printf("<hr><form method=\"POST\" action=\"%s\">\n", selfurl);
	printf("This is a searchable index of information.<br>\n");
	printf("<b>Note:</b> <i>This service can only be used from a forms-");
	printf("capable browser.</i><p>\n");
	printf("Enter keyword(s): ");
	printf("<input type=text name=\"keywords\" value=\"%s\" size=30> ",
	keywords);
	printf("<input type=submit value=\"  Search  \"> ");
	printf("<input type=reset value=\"  Reset  \">\n<p>\n");
	if (indexsources > 1) {
		printf("Select an index to search: ");
		printf("<select name=\"selection\">\n");
		while (sourcelist != NULL) {
			printf("<option%s> %s\n", (lstrstr(sourcelist->line,
			selection)) ? " selected" : "", sourcelist->line);
			sourcelist = sourcelist->next;
		}
		printf("</select>\n<p>\n");
	}
	printf("<input type=hidden name=message value=\"%s\">\n",
	"If you can see this, then your browser can't support hidden fields.");
	printf("<input type=hidden name=source value=\"%s\">\n", source);
	printf("<input type=hidden name=sourcedir value=\"%s\">\n", sourcedir);
	printf("<input type=hidden name=maxhits value=\"%s\">\n", maxhits);
	printf("<input type=hidden name=sorttype value=\"%s\">\n", sorttype);
	printf("<input type=hidden name=host value=\"%s\">\n", host);
	printf("<input type=hidden name=port value=\"%s\">\n", port);
	printf("<input type=hidden name=searchprog value=\"%s\">\n",
	searchprog);
	printf("<input type=hidden name=iconurl value=\"%s\">\n", iconurl);
	printf("<input type=hidden name=useicons value=\"%s\">\n", useicons);
	if (no_options)
		printf("<input type=hidden name=selection value=\"%s\">\n",
		NO_SELECT);
        printf("</form><hr>\n");
}

/* Address authorization failed...
*/

printreject()
{
	printheader();
	printf("<hr>\nSorry, your site either does not have the proper ");
	printf("authorization to use this WAIS index or your ");
	printf("address could not be determined.");
}

/* Grabs a number from a particular field in a line returned
** by waisq or waissearch, usually.
*/

int getnumber(line, field)
     char *line;
     int field;
{
	int i, j, k;

	for (i = k = 0; k != field; k++) {
		for (j = 0; line[i] && !isdigit(line[i]); i++)
			;
		if (!line[i])
			return 0;
		while (isdigit(line[i]))
			j = (j * 10) + (line[i++] - '0');
	}

	return j;
}

/* Grabs filename information from waisq.
*/

char *getqfilename(line)
     char *line;
{
	int i, num, spaces;
	static char filename[MAXSTRLEN];
	char *c;

	c = (char *) strstr(line, "#(") + 2;
	for (i = num = spaces = 0; *c != '\0'; c++) {
		if (isdigit(*c)) {
			while (isdigit(*c)) {
				num = (num * 10) + (*c - '0');
				c++;
			}
			if (spaces >= 2)
				filename[i++] = num;
			if (num == 32)
				spaces++;
			num = 0;
		}
	}
	filename[i] = '\0';
	return filename;
}

/* Grabs filename information returned from waissearch. I don't
** like this very much...
*/

char *getsfilename(line, sdocnum)
     char *line;
     int sdocnum;
{
	int i;
	char *c;
	static char filename[MAXSTRLEN], wholeline[MAXSTRLEN],
	suffix[MAXSTRLEN], prefix[MAXSTRLEN];

	c = (char *) strchr(line, '\'') + 1;
	for (i = 0; *c && i < MAXSTRLEN && *c != '\''; c++)
		wholeline[i++] = *c;
	wholeline[i] = '\0';

	if ((!strchr(wholeline, '/') || !strchr(wholeline, '.')) ||
	sdocnum <= 0)
		return wholeline;

	c = (char *) strchr(line, '\'') + 1;
	for (i = 0; *c && i < MAXSTRLEN && *c != '\'' && !isspace(*c); c++)
		suffix[i++] = *c;
	suffix[i] = '\0';
	if (strstr(suffix, "://"))
		return suffix;

	while (*c && isspace(*c))
		c++;
	for (i = 0; *c && *c != '\'' && !isspace(*c) && i < MAXSTRLEN; c++)
		prefix[i++] = *c;
	prefix[i] = '\0';

	sprintf(filename, "%s%s", prefix, suffix);
	return filename;
}

/* Depending on the suffix, gets the type of file.
*/

char *gettype(filename)
     char *filename;
{
	struct suffixentry *tmplist;
	static char tmpsuffix[MAXSTRLEN], desc[MAXSTRLEN];

	if (!strchr(filename, '.'))
		return unknown_type;

	tmplist = suffixlist;
	while (tmplist != NULL) {
		strcpy(tmpsuffix, strrchr(filename, '.'));
		if (lstrstr(tmpsuffix, tmplist->suffix) &&
		strlen(tmpsuffix) == strlen(tmplist->suffix)) {
			strcpy(desc, tmplist->desc);
			return desc;
		}
		tmplist = tmplist->next;
	}

	return unknown_type;
}

/* Depending on the suffix, gets the right icon URL.
*/

char *geticon(filename)
     char *filename;
{
	struct suffixentry *tmplist;
	static char tmpsuffix[MAXSTRLEN], url[MAXSTRLEN];

	if (!strchr(filename, '.'))
		return unknown_icon;

	tmplist = suffixlist;
	while (tmplist != NULL) {
		strcpy(tmpsuffix, strrchr(filename, '.'));
		if (lstrstr(tmpsuffix, tmplist->suffix) &&
		strlen(tmpsuffix) == strlen(tmplist->suffix)) {
			strcpy(url, tmplist->url);
			return url;
		}
		tmplist = tmplist->next;
	}

	return unknown_icon;
}

/* Depending on the suffix, gets the right MIME type.
*/

char *getmime(filename)
     char *filename;
{
	struct suffixentry *tmplist;
	static char tmpsuffix[MAXSTRLEN], mime[MAXSTRLEN];

	if (!strchr(filename, '.'))
		return unknown_mime;

	tmplist = suffixlist;
	while (tmplist != NULL) {
		strcpy(tmpsuffix, strrchr(filename, '.'));
		if (lstrstr(tmpsuffix, tmplist->suffix) &&
		strlen(tmpsuffix) == strlen(tmplist->suffix)) {
			strcpy(mime, tmplist->mime);
			return mime;
		}
		tmplist = tmplist->next;
	}

	return unknown_mime;
}

/* From the returned filename, tries to extract a title of string of
** some sort for displaying on the page. This is the ugly part.
** If the file is a local HTML file, this extracts the title if
** it exists.
*/

char *gettitle(filename, sdocnum)
     char *filename;
     int sdocnum;
{
	int i, j;
	static char title[MAXTITLELEN], tmpline[MAXSTRLEN];
	FILE *fp;

	if (sdocnum > 0 && strstr(filename, "   ") &&
	strchr(filename, '/')) {
		for (i = j = 0; filename[i] && !isspace(filename[i]); i++)
			tmpline[j++] = filename[i];
		tmpline[j] = '\0';
		return tmpline;
	}
	if (strrchr(filename, '/') && filename[strlen(filename)] != '/')
		strcpy(tmpline, (char *) strrchr(filename, '/') + 1);
	else
		strcpy(tmpline, filename);
	if (!lstrstr(tmpline, ".html"))
		return tmpline;

	if ((fp = fopen(filename, "r")) == NULL)
		return tmpline;
	strcpy(title, parsetitle(fp));
	fclose(fp); 
	if (strlen(title) > 0)
		return title;
	return tmpline;
}

/* Extract information within <title> tags, case insensitive,
** can be over multiple lines...
*/

char *parsetitle(fp)
     FILE *fp;
{
	register char c, *p, *q;
	char title[MAXTITLELEN], tag[MAXTITLELEN];
	int lines, status;

	lines = status = 0;
	p = title;
	*p = '\0';

	for (; lines < TITLETOPLINES ; ) {
		c = getc(fp);
		if (c == '\n')
			lines++;
		if (feof(fp))
			return title;
		switch(c) {
		case '<':
			tag[0] = c;
			status = TI_OPEN;
			for (q = tag + 1;; q++) {
				*q = getc(fp);
				if (*q == '>') {
					q++;
					break;
				}
			}
			*q = '\0';
			q = tag;
			if (lstrstr(q, "</title>")) {
				status = TI_CLOSE;
				*p = '\0';
				return title;
			}
			else {
				if (lstrstr(q, "<title>"))
					status = TI_FOUND;
			}
			break;
		default:
			if (status == TI_FOUND) {
				*p = c;
				p++;
			}
			else {
				if (status == TI_CLOSE)
					return title;
			}
		}
	}
	return title;
}

/* Decodes URL-encoded strings.
*/

char *decode(s)
     char *s;
{
        int i, j, ascint;
        char firstnum, secondnum;
	static char newstr[MAXSTRLEN];

        for (i = j = 0; s[i]; i++) {
                if (s[i] == '%') {
                        firstnum = s[++i];
                        secondnum = s[++i];
                        ascint = (getascii(firstnum) * 16)
                        + getascii(secondnum);
			if (ascint == 10 || ascint == 13)
				ascint = 32;
                        newstr[j++] = (char) ascint;
                        continue;
                }
                else if (s[i] == '+')
			newstr[j++] = ' ';
                else
                        newstr[j++] = s[i];
        }
        newstr[j] = '\0';

        return newstr;
}

/* Returns the right ASCII code for a hex value.
*/

int getascii(hex)
     char hex;
{
        if ((int) hex >= 48 && (int) hex <= 57)
                return (hex - 48);
        else
                return (hex - 65 + 10);
}

/* Encodes strings so they can be put in URLs.
*/

char *encode(s)
     char *s;
{
	int i;
	char hexstr[HEXLEN];
	static char line[MAXSTRLEN];

	for (i = 0; *s; s++)
		if (*s == ' ')
			line[i++] = '+';
		else if (isdigit(*s) || isalpha(*s))
			line[i++] = *s;
		else {
			sprintf(hexstr, "%0X", *s);
			line[i++] = '%';
			line[i++] = hexstr[0];
			line[i] = hexstr[1];

			if (line[i] == '\0') {
				line[i] = line[i - 1];
				line[i - 1] = '0';
			}

			i++;
		}
	line[i] = '\0';

	return line;
}

/* lstrstr() is the Boyer-Moore algorithm from Sedgewick. Where else? :)
*/

initskip(s, len)
     char *s;
     int len;
{
        int i, j;

        for (i = 0; i < MAXSTRLEN; i++)
                skip[i] = len;
        for (j = 0; j <= len - 1; j++)
                skip[lstrindex(s[j])] = len - j - 1;
}

int lstrindex(c)
     char c;
{
        char d;

        d = tolower(c);
        if (d >= 'a' && d <= 'z')
                return (d - 'a' + 1);
        else
                return 0;
}

char *lstrstr(s, t)
     char *s;
     char *t;
{
        int i, j, k, slen, tlen;

        slen = strlen(s);
        tlen = strlen(t);
        initskip(t, tlen);

        for (i = j = tlen - 1; j > 0; i--, j--) {
                while (tolower(s[i]) != tolower(t[j])) {
                        k = skip[lstrindex(s[i])];
                        i += (tlen - j > k) ? tlen - j : k;
                        if (i >= slen)
                                return NULL;
                        j = tlen - 1;
                }
        }
        return (s + i);
}

/* Add information returned from waisq and waissearch into a binary tree
** so it can be sorted and displayed nicely.
*/

struct entry *addentry(e, score, lines, bytes, filename, title, type, icon,
docnum)
     struct entry *e;
     int score;
     int lines;
     int bytes;
     char *filename;
     char *title;
     char *type;
     char *icon;
     int docnum;
{
	int isbigger;

	if (strstr(filename, "--------"))
		return e;
	if (e == NULL) {
		e = (struct entry *) emalloc(sizeof(struct entry));
		e->score = score;
		e->lines = lines;
		e->bytes = bytes;
		e->docnum = docnum;
		e->filename = (char *) mystrdup(filename);
		e->title = (char *) mystrdup(title);
		e->type = (char *) mystrdup(type);
		e->icon = (char *) mystrdup(icon);
		e->left = e->right = NULL;
	}
	else {
		if (!strcmp(sorttype, "score"))
			isbigger = (e->score >= score) ? 1 : 0;
		else if (!strcmp(sorttype, "lines"))
			isbigger = (e->lines >= lines) ? 1 : 0;
		else if (!strcmp(sorttype, "bytes"))
			isbigger = (e->bytes >= bytes) ? 1 : 0;
		else if (!strcmp(sorttype, "title"))
			isbigger = (strcmp(e->title, title) < 0) ? 1 : 0;
		else if (!strcmp(sorttype, "type"))
			isbigger = (strcmp(e->type, type) < 0) ? 1 : 0;

		if (isbigger)
			e->left = addentry(e->left, score, lines, bytes,
			filename, title, type, icon, docnum);
		else
			e->right = addentry(e->right, score, lines, bytes,
			filename, title, type, icon, docnum);
	}

	return e;
}

/* Prints everything in the entry structures and adds extra information
** in the URLs so we know what to do next time.
*/

printentries(e)
     struct entry *e;
{
	static int number;
	static char newfilename[MAXSTRLEN], tmpfilename[MAXSTRLEN];

	if (e != NULL) {
		printentries(e->right);
		number++;
		printf("<dt> <b>%d:  </b>", number);
		if (use_icons)
			printf("<img src=\"%s\" align=\"bottom\" alt=\"\">\n ",
			e->icon);

		if (ruleslist != NULL) {
			strcpy(newfilename, ruleparse(e->filename));
			strcpy(tmpfilename, newfilename);
			sprintf(newfilename, replace(tmpfilename,
			KEYWORDVAR, encode(keywords)));
		}
		else
			strcpy(newfilename, e->filename);

		if (lstrstr(e->filename, ".src")) {
			strcpy(tmpfilename, encode(keywords));
			printf("<a href=\"%s?%s%s&%s%s&%s%s&%s%s&%s%s&%s%s",
			selfurl, DESCTXT, "yes", HOSTTXT, host,
			PORTTXT, port, SOURCETXT, source, SEARCHPROGTXT,
			"waissearch", SELECTTXT, NO_SELECT);
			printf("&%s%d&%s%s\">", DOCNUMTXT, e->docnum,
			KEYWORDSTXT, tmpfilename);
		}
		else if (e->docnum != -1 && !strstr(newfilename, "://")) {
			strcpy(tmpfilename, encode(keywords));
			printf("<a href=\"%s?%s%s&%s%s&%s%s&%s%s&%s%s&",
			selfurl, DESCTXT, "yes", HOSTTXT, host,
			PORTTXT, port, SOURCETXT, source, SEARCHPROGTXT,
			"waissearch");
			printf("%s%s&%s%d&%s%s\">", SELECTTXT, NO_SELECT,
			DOCNUMTXT, e->docnum, KEYWORDSTXT, tmpfilename);
		}
		else
			printf("<a href=\"%s\">", newfilename);

		printf("%s</a>\n", e->title);
		printf("<dd> Score: <b>%d</b>", e->score);
		if (e->lines >= 0)
			printf(", Lines: <b>%d</b>", e->lines);
		if (e->bytes < 0)
			printf("");
		else if (e->bytes <= 1000)
			printf(", Size: <b>%d bytes", e->bytes);
		else
			printf(", Size: <b>%d kbytes", e->bytes / 1000);
		printf("</b>");
		printf(", Type: <b>%s</b>\n", e->type);
		printentries(e->left);
	}
}

/* Parses lines according to the SourceRules directives.
*/

char *ruleparse(line)
     char *line;
{
	char rule[MAXSTRLEN];
	static char tmpline[MAXSTRLEN], newtmpline[MAXSTRLEN];
	static char line1[MAXSTRLEN], line2[MAXSTRLEN];
	struct source *tmplist;

	tmplist = ruleslist;
	strcpy(tmpline, line);
	while(1) {
		strcpy(rule, getrule());
		if (rule[0] == '\0') {
			ruleslist = tmplist;
			return tmpline;
		}
		else {
			if (lstrstr(rule, "replace")) {
				strcpy(line1, getrule());
				strcpy(line2, getrule());
				strcpy(newtmpline, replace(tmpline,
				line1, line2));
			}
			else if (lstrstr(rule, "append"))
				sprintf(newtmpline, "%s%s", tmpline, getrule());
			else if (lstrstr(rule, "prepend"))
				sprintf(newtmpline, "%s%s", getrule(), tmpline);
			strcpy(tmpline, newtmpline);
		}
	}
}

/* Grabs the next rule from the list.
*/

char *getrule()
{
	static char rule[MAXSTRLEN];

	if (ruleslist == NULL)
		return "\0";
	strcpy(rule, ruleslist->line);
	ruleslist = ruleslist->next;

	return rule;
}

/* Couldn't find something...
*/

notfound()
{
	printf("Sorry, I didn't find any documents that matched your ");
	printf("search for \"<b>%s</b>\"!", keywords);
}

/* What is the physical size of a file?
*/

int getsize(path)
     char *path;
{
        struct stat stbuf;

        if (stat(path, &stbuf))
                return -1;
        return stbuf.st_size;
}

/* Does a file exist?
*/

int isfile(path)
     char *path;
{
        struct stat stbuf;

        if (stat(path, &stbuf))
                return 0;
        return ((stbuf.st_mode & S_IFMT) == S_IFREG) ? 1 : 0;
}

/* Split up multiple address masks and check them.
*/

int isokstring(string, mask)
     char *string;
     char *mask;
{
	int i;
	char *s, *t;

	if (lstrstr(mask, "all"))
		return 1;

	i = 0;
	t = (char *) mystrdup(mask);

	while (1) {
		s = (char *) strtok((i++) ? NULL : t, ",");
		if (s == NULL)
			break;
		if (isinname(string, s)) {
			free(t);
			return 1;
		}
	}
	free(t);

	return 0;
}

/* Does an address mask fit the user's address?
*/

int isinname(string, mask)
     char *string;
     char *mask;
{
        int i, j;
        char firstchar, lastchar, *tempmask;

        if (!strcmp(mask, "*"))
                return 1;

        firstchar = mask[0];
        lastchar = mask[(strlen(mask) - 1)];
        tempmask = (char *) emalloc(strlen(mask));

        for (i = j = 0; mask[i]; i++)
                if (mask[i] != '*')
                        tempmask[j++] = mask[i];
        tempmask[j] = '\0';

        if (firstchar == '*') {
                if (lastchar == '*') {
                        if ((char *) strstr(string, tempmask)) {
				free(tempmask);
                                return 1;
			}
                }
                else {
                        if ((char *) strstr(string, tempmask) ==
                        string + strlen(string) - strlen(tempmask)) {
				free(tempmask);
                                return 1;
			}
                }
        }
        else if (lastchar == '*') {
                if ((char *) strstr(string, tempmask) == string) {
			free(tempmask);
                        return 1;
		}
        }
        else {
                if (!strcmp(string, tempmask)) {
			free(tempmask);
                        return 1;
		}
        }
	free(tempmask);

        return 0;
}

/* Reads the config file and grabs all the values it can find.
*/

getdefaults()
{
	int found, skiplen;
	char *c, line[MAXSTRLEN], value[MAXSTRLEN], sourcepath[MAXSTRLEN],
	lineselection[MAXSTRLEN], suffix[MAXSTRLEN], desc[MAXSTRLEN],
	url[MAXSTRLEN], mime[MAXSTRLEN], rule[MAXSTRLEN];
	FILE *fp;

	found = 0;
	strcpy(lineselection, selection);
	if ((fp = fopen(CONFFILE, "r")) == NULL)
		progerr("Couldn't open configuration file.");
	while (fgets(line, MAXSTRLEN, fp) != NULL) {
		if (line[0] == '#' || line[0] == '\n')
			continue;
		if (getconfvalue(line, "pagetitle", value) != NULL)
			strcpy(pagetitle, value);
		else if (getconfvalue(line, "selfurl", value) != NULL)
			strcpy(selfurl, value);
		else if (getconfvalue(line, "maxhits", value) != NULL)
			strcpy(maxhits, value);
		else if (getconfvalue(line, "addrmask", value) != NULL)
			strcpy(addrmask, value);
		else if (getconfvalue(line, "iconurl", value) != NULL)
			strcpy(iconurl, value);
		else if (getconfvalue(line, "sorttype", value) != NULL)
			strcpy(sorttype, value);
		else if (getconfvalue(line, "swishbin", value) != NULL)
			strcpy(swishbin, value);
		else if (getconfvalue(line, "waisqbin", value) != NULL)
			strcpy(waisqbin, value);
		else if (getconfvalue(line, "waissearchbin", value) != NULL)
			strcpy(waissearchbin, value);
		else if (getconfvalue(line, "useicons", value) != NULL)
			strcpy(useicons, value);
		else if ((c = (char *) lstrstr(line, "sourcerules")) &&
		found == 1) {
			c += strlen("sourcerules");
			while (1) {
				strcpy(rule, getword(c, &skiplen));
				if (!skiplen | rule[0] == '\0' ||
				rule[0] == '\n')
					break;
				else {
					c += skiplen;
					ruleslist = (struct source *)
					addsource(ruleslist, rule);
				}
			}
		}
		else if (c = (char *) lstrstr(line, "typedef")) {
			c += strlen("typedef");
			strcpy(suffix, getword(c, &skiplen));
			c += skiplen;
			strcpy(desc, getword(c, &skiplen));
			c += skiplen;
			strcpy(url, getword(c, &skiplen));
			c += skiplen;
			strcpy(mime, getword(c, &skiplen));
			suffixlist = (struct suffixentry *)
			addsuffix(suffixlist, suffix, desc, url, mime);
		}
		else if (c = (char *) lstrstr(line, "waissource")) {
			if (no_options)
				continue;
			indexsources++;
			sourcelist = (struct source *)
			addsource(sourcelist, getdesc(line));
			if (found > 0) {
				found++;
				continue;
			}
			if ((lineselection[0] == '\0') ||
			lstrstr(line, lineselection))
				found = use_selection = 1;
			if (iswqsource(line)) {
				strcpy(searchprog, "waisq");
				c += strlen("waissource");
				strcpy(sourcepath, getword(c, &skiplen));
				c += skiplen;
				strcpy(selection, getword(c, &skiplen));

				if (strchr(sourcepath, '/') == NULL) {
					sourcedir[0] = '\0';
					strcpy(source, sourcepath);
				}
				else {
					strcpy(source,
					strrchr(sourcepath, '/') + 1);
					strncpy(sourcedir, sourcepath,
					strlen(sourcepath) - strlen(source));
				}
			}
			else {
				strcpy(searchprog, "waissearch");
				strcpy(sourcedir, "");
				c += strlen("waissource");
				strcpy(host, getword(c, &skiplen));
				c += skiplen;
				strcpy(port, getword(c, &skiplen));
				c += skiplen;
				strcpy(source, getword(c, &skiplen));
				c += skiplen;
				strcpy(selection, getword(c, &skiplen));
			}
		}
		else if (c = (char *) lstrstr(line, "swishsource")) {
			if (no_options)
				continue;
			indexsources++;
			sourcelist = (struct source *)
			addsource(sourcelist, getdesc(line));
			if (found > 0) {
				found++;
				continue;
			}
			if ((lineselection[0] == '\0') ||
			lstrstr(line, lineselection))
				found = use_selection = 1;
			strcpy(searchprog, "swish");
			c += strlen("swishsource");
			strcpy(sourcepath, getword(c, &skiplen));
			c += skiplen;
			strcpy(selection, getword(c, &skiplen));

			if (strchr(sourcepath, '/') == NULL) {
				sourcedir[0] = '\0';
				strcpy(source, sourcepath);
			}
			else {
				strcpy(source,
				strrchr(sourcepath, '/') + 1);
				strncpy(sourcedir, sourcepath,
				strlen(sourcepath) - strlen(source));
			}
		}
	}
	fclose(fp);
}

/* Is a waissource line describing a waisq source or a waissearch source?
*/

int iswqsource(line)
     char *line;
{
	int skiplen;
	char *c, tmpline[MAXSTRLEN];

	c = line;
	c += strlen("waissource");
	strcpy(tmpline, getword(c, &skiplen));
	c += skiplen;
	strcpy(tmpline, getword(c, &skiplen));

	if (isnum(tmpline))
		return 0;
	else
		return 1;
}

/* Is a string a number?
*/

int isnum(line)
     char *line;
{
	int i;

	for (i = 0; line[i]; i++)
		if (!isdigit(line[i]))
			return 0;
	return 1;
}

/* Grabs values from lines with variables. If the line is in quotes,
** spaces are counted as part of the value.
*/

char *getconfvalue(line, var, value)
     char *line;
     char *var;
     char *value;
{
        int i;
        char *c;
        static char tmpvalue[MAXSTRLEN];

        if ((c = (char *) lstrstr(line, var)) != NULL) {
		if (c != line)
			return NULL;
                c += strlen(var);
                while (isspace(*c) || *c == '\"')
                        c++;
                if (*c == '\0')
                        return NULL;
                for (i = 0; *c != '\0' && *c != '\"' && *c != '\n' &&
                i < MAXSTRLEN; c++)
                        tmpvalue[i++] = *c;
                tmpvalue[i] = '\0';
                strcpy(value, tmpvalue);
                return tmpvalue;
        }
        else
                return NULL;
}

/* Grabs a source description (for the pop-up menu) for sources
** specified in the config file.
*/

char *getdesc(line)
     char *line;
{
	int i;
	char *c;
	static char desc[MAXSTRLEN];

	if ((c = (char *) strchr(line, '\"')) == NULL)
		return NO_DESC;
	c++;
	for (i = 0; *c && *c != '\"' && i < MAXSTRLEN; c++)
		desc[i++] = *c;
	desc[i] = '\0';

	return desc;
}

/* Generic list for storing files, source descriptions, and rules.
*/

struct source *addsource(sp, line)
     struct source *sp;
     char *line;
{
        struct source *tempnode, *newnode;

        newnode = (struct source *) emalloc(sizeof(struct source));
        newnode->line = (char *) mystrdup(line);
        newnode->next = NULL;

        if (sp == NULL)
                sp = newnode;
        else {
                for (tempnode = sp; tempnode->next != NULL; tempnode =
                tempnode->next)
                        ;
                tempnode->next = newnode;
        }

        return sp;
}

/* The list that holds all the suffixes for checking against filenames.
*/

struct suffixentry *addsuffix(sp, suffix, desc, url, mime)
     struct suffixentry *sp;
     char *suffix;
     char *desc;
     char *url;
     char *mime;
{
        struct suffixentry *tempnode, *newnode;

        newnode = (struct suffixentry *) emalloc(sizeof(struct suffixentry));
        newnode->suffix = (char *) mystrdup(suffix);
        newnode->desc = (char *) mystrdup(desc);
        newnode->url = (char *) mystrdup(replace(url, ICONVAR, iconurl));
        newnode->mime = (char *) mystrdup(mime);
        newnode->next = NULL;

	if (!strcmp(suffix, ".??")) {
		strcpy(unknown_icon, replace(url, ICONVAR, iconurl));
		strcpy(unknown_type, desc);
		strcpy(unknown_mime, mime);
	}

        if (sp == NULL)
                sp = newnode;
        else {
                for (tempnode = sp; tempnode->next != NULL; tempnode =
                tempnode->next)
                        ;
                tempnode->next = newnode;
        }

        return sp;
}

/* In a string, replaces all occurrences of "oldpiece" with "newpiece".
** Not sure if this is bulletproof yet.
*/

char *replace(string, oldpiece, newpiece)
     char *string;
     char *oldpiece;
     char *newpiece;
{
        int i, j, limit;
        char *c;
        char beforestring[MAXSTRLEN], afterstring[MAXSTRLEN];
        static char newstring[MAXSTRLEN];

        if ((c = (char *) strstr(string, oldpiece)) == NULL)
                return string;
        limit = c - string;

        for (i = 0; i < limit; i++)
                beforestring[i] = string[i];
        beforestring[i] = '\0';

        i += strlen(oldpiece);

        for (j = 0; string[i] != '\0'; i++)
                afterstring[j++] = string[i];
        afterstring[j] = '\0';

        sprintf(newstring, "%s%s%s", beforestring, newpiece, afterstring);

        while (strstr(newstring, oldpiece))
                strcpy(newstring, replace(newstring, oldpiece, newpiece));

        return newstring;
}

/* From a line, grabs the next word and returns it. If the word is
** in quotes, spaces (and other characters) are counted as part of the word.
*/

char *getword(line, skiplen)
     char *line;
     int *skiplen;
{
	int i, inquotes;
	char *start;
	static char word[MAXSTRLEN];

	start = line;
	if (!(*line))
		return "\0";
	while (isspace(*line))
		line++;
	if (!(*line))
		return "\0";
	if (*line == '\"') {
		inquotes = 1;
		line++;
	}
	else
		inquotes = 0;
	for (i = 0; *line && i < MAXSTRLEN &&
	((inquotes) ? (*line != '\"') : (!isspace(*line))); line++)
		word[i++] = *line;
	word[i] = '\0';
	if (!(*line))
		return "\0";
	if (*line == '\"')
		line++;

	*skiplen = line - start;

	return word;
}

/* How many times does the a character appear in a line?
*/

int numchar(line, c)
     char *line;
     char c;
{
	int i;

	for (i = 0; *line; line++)
		if (*line == c)
			i++;
	return i;
}

/* Error messages for broken pipes, SIGSEGV, and timing out.
*/

void badconnection()
{
	progerr("Can't connect to this WAIS server (it is probably down).");
}

void badsegviol()
{
	progerr("Caught a memory violation (probably a parsing error).");
}

void badtimeout()
{
	char message[MAXSTRLEN];

	sprintf(message,
	"This service timed out (%d seconds) - it may be too busy.",
	TIMEOUT);
	progerr(message);
}
