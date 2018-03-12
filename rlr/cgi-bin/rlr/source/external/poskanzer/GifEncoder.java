// GifEncoder - write out an image as a GIF
//
// Copyright (C) 1996 by Jef Poskanzer <jef@acme.com>.  All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
// OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE.
//
// Visit the ACME Labs Java page for up-to-date versions of this and other
// fine Java utilities: http://www.acme.com/java/
//
// This package was modified by Leemon Baird, 5 March 97, to generate /*lcb*/
// animated GIFs.  To do that, call setFirstFrame() before outputting /*lcb*/
// the first frame of the animation, setLastFrame() before outputting /*lcb*/
// the last frame of the animation, and setMiddleFrame() for all the  /*lcb*/
// frames in between.  Lines added or changed (other than             /*lcb*/
// indentation) are suffixed with /*lcb*/.                            /*lcb*/
// These changes are free to redistribute/modify/mangle etc.          /*lcb*/
// Leemon Baird, leemon@cs.cmu.edu, http://www.cs.cmu.edu/~baird      /*lcb*/

package external.poskanzer;

import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.image.*;

/// Write out an image as a GIF.
// <P>
// <A HREF="/resources/classes/Acme/GifEncoder.java">Fetch the software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.Z">Fetch the entire Acme package.</A>

public class GifEncoder extends ImageEncoder
    {

    /// Constructor.
    // @param img The image to encode.
    // @param out The stream to write the GIF to.
    public GifEncoder( Image img, OutputStream out )
	{
	super( img, out );
	}

    /// Constructor.
    // @param prod The ImageProducer to encode.
    // @param out The stream to write the GIF to.
    public GifEncoder( ImageProducer prod, OutputStream out )
	{
	super( prod, out );
	}

    boolean interlace         = false;
    boolean standardColorMap  = false;//don't adapt colors   /*lcb*/
    boolean isFirst           = true; //first or only frame  /*lcb*/
    boolean isLast            = true; //last  or only frame  /*lcb*/
                                                             /*lcb*/
    /// This is the first frame of an animated GIF           /*lcb*/
    public void setFirstFrame()                              /*lcb*/
        {                                                    /*lcb*/
        isFirst           = true;                            /*lcb*/
        isLast            = false;                           /*lcb*/
        standardColorMap  = true;                            /*lcb*/
        }                                                    /*lcb*/
                                                             /*lcb*/
    /// This is the last frame of an animated GIF            /*lcb*/
    public void setLastFrame()                               /*lcb*/
        {                                                    /*lcb*/
        isFirst           = false;                           /*lcb*/
        isLast            = true;                            /*lcb*/
        standardColorMap  = true;                            /*lcb*/
        }                                                    /*lcb*/
                                                             /*lcb*/
    /// This is a middle frame (not first/last) of animation /*lcb*/
    public void setMiddleFrame()                             /*lcb*/
        {                                                    /*lcb*/
        isFirst           = false;                           /*lcb*/
        isLast            = false;                           /*lcb*/
        standardColorMap  = true;                            /*lcb*/
        }                                                    /*lcb*/
                                                             /*lcb*/
    /// This is a nonanimated GIF (one frame only)           /*lcb*/
    public void setOnlyFrame()                               /*lcb*/
        {                                                    /*lcb*/
        isFirst           = true;                            /*lcb*/
        isLast            = true;                            /*lcb*/
        standardColorMap  = false;                           /*lcb*/
        }                                                    /*lcb*/
                                                             /*lcb*/
    /// Make this an interlaced GIF.
    public void setInterlace( boolean interlace )
        {
        this.interlace = interlace;                          /*lcb*/
        }

    int width, height;
    int[] rgbPixels;
    
    void encodeStart( int width, int height ) throws IOException
	{
	this.width = width;
	this.height = height;
	rgbPixels = new int[width * height];
	}

    void encodeRgb(
	int x, int y, int w, int h, int[] rgbPixels, int off, int scansize )
	throws IOException
	{
	// Save the pixels.
	for ( int row = 0; row < h; ++row )
	    {
	    int srcRowOff = off + row * scansize;
	    int dstRowOff = ( y + row ) * width + x;
	    //for ( int col = 0; col < w; ++col )
            //    {
            //    int i = srcRowOff + col;
            //    int j = dstRowOff + col;
            //    this.rgbPixels[j] = rgbPixels[i];
            //    }
	    System.arraycopy(
		rgbPixels, srcRowOff, this.rgbPixels, dstRowOff, w );
            }

	}
    
    Hashtable colorHash;

    void encodeDone() throws IOException
	{
        if (standardColorMap)                                /*lcb*/
            {                                                /*lcb*/
            GIFEncode(out, width, height, interlace,         /*lcb*/
                      (byte) 0, -1, 8, null, null, null);    /*lcb*/
            return;                                          /*lcb*/
            }                                                /*lcb*/

        // Put all the pixels into a hash table.
        colorHash = new Hashtable();
	int index = 0;
        for ( int row = 0; row < height; ++row )
            {
            int rowOffset = row * width;
            for ( int col = 0; col < width; ++col )
                {
                int i = rowOffset + col;
                int rgb = rgbPixels[i];
                Integer pixel = new Integer( rgb );
                GifEncoderHashitem item =
                    (GifEncoderHashitem) colorHash.get( pixel );
                if ( item == null )
                    {
                    if ( index >= 256 )
                        { //too many colors                 /*lcb*/
                        standardColorMap = true;            /*lcb*/
                        GIFEncode(out, width, height,       /*lcb*/
                                  interlace, (byte) 0,      /*lcb*/
                                  -1, 8, null, null, null); /*lcb*/
                        return;                             /*lcb*/
                        }                                   /*lcb*/
                    item = new GifEncoderHashitem( rgb, 1, (byte) index );
                    ++index;
                    colorHash.put( pixel, item );
                    }
                else
                    ++item.count;
                }
            }

	// Turn that into colormap entries.
	byte[] reds = new byte[256];
	byte[] grns = new byte[256];
	byte[] blus = new byte[256];
	for ( Enumeration e = colorHash.elements(); e.hasMoreElements(); )
	    {
	    GifEncoderHashitem item = (GifEncoderHashitem) e.nextElement();
	    reds[item.index] = (byte) ( ( item.rgb >> 16 ) & 0xff );
	    grns[item.index] = (byte) ( ( item.rgb >>  8 ) & 0xff );
	    blus[item.index] = (byte) (   item.rgb         & 0xff );
	    }

	GIFEncode(
            out, width, height, interlace, (byte) 0, -1, 8, reds, grns, blus );
	}
    
    byte GetPixel( int x, int y ) throws IOException
	{
        if (standardColorMap)  //find index for pixel              /*lcb*/
            { //this ignores the last 40 colormap entries          /*lcb*/
            int rgb=rgbPixels[y * width + x];                      /*lcb*/
            return (byte)(.5                                       /*lcb*/
                 +36*(int)(.5+(255.-((rgb >> 16) & 0xff))/255*5)   /*lcb*/
                 + 6*(int)(.5+(255.-((rgb >>  8) & 0xff))/255*5)   /*lcb*/
                 +   (int)(.5+(255.-( rgb        & 0xff))/255*5)); /*lcb*/
            }                                                      /*lcb*/

	Integer pixel = new Integer( rgbPixels[y * width + x] );
	GifEncoderHashitem item =
	    (GifEncoderHashitem) colorHash.get( pixel );
	if ( item == null )
	    throw new IOException( "color not found" );
	return item.index;
	}

    static void writeString( OutputStream out, String str ) throws IOException
        {
        int len = str.length();
        byte[] buf = new byte[len];
        str.getBytes( 0, len, buf, 0 );
        out.write( buf );
        }

    // Adapted from ppmtogif, which is based on GIFENCOD by David
    // Rowley <mgardi@watdscu.waterloo.edu>.  Lempel-Zim compression
    // based on "compress".

    int Width, Height;
    boolean Interlace;
    int curx, cury;
    int CountDown;
    int Pass = 0;

    void GIFEncode(
        OutputStream outs, int Width, int Height, boolean Interlace, byte Background, int Transparent, int BitsPerPixel, byte[] Red, byte[] Green, byte[] Blue )
	throws IOException
	{
	byte B;
	int LeftOfs, TopOfs;
	int ColorMapSize;
	int InitCodeSize;
	int i;

	this.Width = Width;
	this.Height = Height;
	this.Interlace = Interlace;
	ColorMapSize = 1 << BitsPerPixel;
	LeftOfs = TopOfs = 0;

	// Calculate number of bits we are expecting
	CountDown = Width * Height;

	// Indicate which pass we are on (if interlace)
	Pass = 0;

        // The initial code size
        if ( BitsPerPixel <= 1 )
            InitCodeSize = 2;
        else
            InitCodeSize = BitsPerPixel;

        // Set up the current x and y position
        curx = 0;
        cury = 0;


        if (isFirst) //first or only animation frame        /*lcb*/
            {        //so write out the header info         /*lcb*/
            // Write the Magic header
            writeString( outs, "GIF89a" );

            // Write out the screen width and height
            Putword( Width, outs );
            Putword( Height, outs );

            // Indicate that there is a global colour map
            B = (byte) 0x80;                // Yes, there is a color map
            // OR in the resolution
            B |= ( 8 - 1 ) << 4;
            // Not sorted
            // OR in the Bits per Pixel
            B |= ( BitsPerPixel - 1 );

            // Write it out
            Putbyte( B, outs );

            // Write out the Background colour
            Putbyte( Background, outs );

            // Pixel aspect ratio - 1:1.
            Putbyte( (byte) 49, outs );

            // Write out the Global Colour Map
            if (!standardColorMap) //use calculated colors       /*lcb*/
                for ( i = 0; i < ColorMapSize; ++i )
                    {
                    Putbyte( Red  [i], outs );
                    Putbyte( Green[i], outs );
                    Putbyte( Blue [i], outs );
                    }
            else    //use standard colors for animations         /*lcb*/
                {                                                /*lcb*/
                for (int r=0;r<6;r++)                            /*lcb*/
                    for (int g=0;g<6;g++)                        /*lcb*/
                        for (int b=0;b<6;b++)                    /*lcb*/
                            {                                    /*lcb*/
                            Putbyte((byte)(255.5-r*255/5),outs); /*lcb*/
                            Putbyte((byte)(255.5-g*255/5),outs); /*lcb*/
                            Putbyte((byte)(255.5-b*255/5),outs); /*lcb*/
                            }                                    /*lcb*/
                for (int j=1;j<=10;j++)                          /*lcb*/
                  {                                              /*lcb*/
                  Putbyte((byte)(.5+j*255./11),outs);            /*lcb*/
                  Putbyte((byte)(.5+j*255./11),outs);            /*lcb*/
                  Putbyte((byte)(.5+j*255./11),outs);            /*lcb*/
                  }                                              /*lcb*/
                for (int j=1;j<=10;j++)                          /*lcb*/
                  {                                              /*lcb*/
                  Putbyte((byte)(.5+j*255./11),outs);            /*lcb*/
                  Putbyte((byte)0,outs);                         /*lcb*/
                  Putbyte((byte)0,outs);                         /*lcb*/
                  }                                              /*lcb*/
                for (int j=1;j<=10;j++)                          /*lcb*/
                  {                                              /*lcb*/
                  Putbyte((byte)0,outs);                         /*lcb*/
                  Putbyte((byte)(.5+j*255./11),outs);            /*lcb*/
                  Putbyte((byte)0,outs);                         /*lcb*/
                  }                                              /*lcb*/
                for (int j=1;j<=10;j++)                          /*lcb*/
                  {                                              /*lcb*/
                  Putbyte((byte)0,outs);                         /*lcb*/
                  Putbyte((byte)0,outs);                         /*lcb*/
                  Putbyte((byte)(.5+j*255./11),outs);            /*lcb*/
                  }                                              /*lcb*/
                // Netscape Extension for looping                /*lcb*/
                Putbyte((byte)0x21, outs );                      /*lcb*/
                Putbyte((byte)0xFF, outs );                      /*lcb*/
                Putbyte((byte)0x0B, outs );                      /*lcb*/
                writeString(outs, "NETSCAPE2.0");                /*lcb*/
                Putbyte((byte)0x03, outs );                      /*lcb*/
                Putbyte((byte)0x01, outs );                      /*lcb*/
                Putbyte((byte)0x00, outs );//LSB # loops         /*lcb*/
                Putbyte((byte)0x00, outs );//MSB # loops         /*lcb*/
                Putbyte((byte)0x00, outs );                      /*lcb*/
                }                                                /*lcb*/
            } //end of start of file                             /*lcb*/
    
        // Write out extension for transparent colour index, if necessary.
//        if ( Transparent != -1 )                               /*lcb*/
//            {                                                  /*lcb*/
        Putbyte( (byte) '!', outs );
        Putbyte( (byte) 0xf9, outs );
        Putbyte( (byte) 4, outs );
        Putbyte( (byte) (Transparent==-1 ? 4 : 5), outs );       /*lcb*/
        Putbyte( (byte) 30,outs );  //LSB: delay per frame in    /*lcb*/
        Putbyte( (byte) 0, outs );  //MSB:       1/100 second    /*lcb*/
        Putbyte( (byte) Transparent, outs );
        Putbyte( (byte) 0, outs );
//            }                                                  /*lcb*/

        // Write an Image separator
        Putbyte( (byte) ',', outs );

	// Write the Image header
	Putword( LeftOfs, outs );
        Putword( TopOfs,  outs );
        Putword( Width,   outs );
        Putword( Height,  outs );

	// Write out whether or not the image is interlaced
	if ( Interlace )
            Putbyte( (byte) 0x40, outs );
	else
            Putbyte( (byte) 0x00, outs );

	// Write out the initial code size
	Putbyte( (byte) InitCodeSize, outs );

	// Go and actually compress the data
	compress( InitCodeSize+1, outs );

	// Write out a Zero-length packet (to end the series)
	Putbyte( (byte) 0, outs );

        if (isLast) //last or only animation frame               /*lcb*/
            // Write the GIF file terminator
            Putbyte( (byte) ';', outs );
	}

    // Bump the 'curx' and 'cury' to point to the next pixel
    void BumpPixel()
	{
	// Bump the current X position
	++curx;

	// If we are at the end of a scan line, set curx back to the beginning
	// If we are interlaced, bump the cury to the appropriate spot,
	// otherwise, just increment it.
	if ( curx == Width )
	    {
	    curx = 0;

	    if ( ! Interlace )
		++cury;
	    else
		{
		switch( Pass )
		    {
		    case 0:
		    cury += 8;
		    if ( cury >= Height )
			{
			++Pass;
			cury = 4;
			}
		    break;

		    case 1:
		    cury += 8;
		    if ( cury >= Height )
			{
			++Pass;
			cury = 2;
			}
		    break;

		    case 2:
		    cury += 4;
		    if ( cury >= Height )
			{
			++Pass;
			cury = 1;
			}
		    break;

		    case 3:
		    cury += 2;
		    break;
		    }
		}
	    }
	}

    static final int EOF = -1;

    // Return the next pixel from the image
    int GIFNextPixel() throws IOException
	{
        byte r;

	if ( CountDown == 0 )
	    return EOF;

	--CountDown;

	r = GetPixel( curx, cury );

	BumpPixel();

        return r<0 ? (r+256) : r; //was return r                 /*lcb*/
	}

    // Write out a word to the GIF file
    void Putword( int w, OutputStream outs ) throws IOException
	{
	Putbyte( (byte) ( w & 0xff ), outs );
	Putbyte( (byte) ( ( w >> 8 ) & 0xff ), outs );
	}

    // Write out a byte to the GIF file
    void Putbyte( byte b, OutputStream outs ) throws IOException
	{
	outs.write( b );
	}


    // GIFCOMPR.C       - GIF Image compression routines
    //
    // Lempel-Ziv compression based on 'compress'.  GIF modifications by
    // David Rowley (mgardi@watdcsu.waterloo.edu)

    // General DEFINEs

    static final int BITS = 12;

    static final int HSIZE = 5003;		// 80% occupancy

    // GIF Image compression - modified 'compress'
    //
    // Based on: compress.c - File compression ala IEEE Computer, June 1984.
    //
    // By Authors:  Spencer W. Thomas      (decvax!harpo!utah-cs!utah-gr!thomas)
    //              Jim McKie              (decvax!mcvax!jim)
    //              Steve Davies           (decvax!vax135!petsd!peora!srd)
    //              Ken Turkowski          (decvax!decwrl!turtlevax!ken)
    //              James A. Woods         (decvax!ihnp4!ames!jaw)
    //              Joe Orost              (decvax!vax135!petsd!joe)

    int n_bits;				// number of bits/code
    int maxbits = BITS;			// user settable max # bits/code
    int maxcode;			// maximum code, given n_bits
    int maxmaxcode = 1 << BITS; // should NEVER generate this code

    final int MAXCODE( int n_bits )
	{
	return ( 1 << n_bits ) - 1;
	}

    int[] htab = new int[HSIZE];
    int[] codetab = new int[HSIZE];

    int hsize = HSIZE;		// for dynamic table sizing

    int free_ent = 0;			// first unused entry

    // block compression parameters -- after all codes are used up,
    // and compression rate changes, start over.
    boolean clear_flg = false;

    // Algorithm:  use open addressing double hashing (no chaining) on the
    // prefix code / next character combination.  We do a variant of Knuth's
    // algorithm D (vol. 3, sec. 6.4) along with G. Knott's relatively-prime
    // secondary probe.  Here, the modular division first probe is gives way
    // to a faster exclusive-or manipulation.  Also do block compression with
    // an adaptive reset, whereby the code table is cleared when the compression
    // ratio decreases, but after the table fills.  The variable-length output
    // codes are re-sized at this point, and a special CLEAR code is generated
    // for the decompressor.  Late addition:  construct the table according to
    // file size for noticeable speed improvement on small files.  Please direct
    // questions about this implementation to ames!jaw.

    int g_init_bits;

    int ClearCode;
    int EOFCode;

    void compress( int init_bits, OutputStream outs ) throws IOException
	{
	int fcode;
	int i /* = 0 */;
	int c;
	int ent;
	int disp;
	int hsize_reg;
	int hshift;

	// Set up the globals:  g_init_bits - initial number of bits
	g_init_bits = init_bits;

	// Set up the necessary values
	clear_flg = false;
	n_bits = g_init_bits;
	maxcode = MAXCODE( n_bits );

	ClearCode = 1 << ( init_bits - 1 );
	EOFCode = ClearCode + 1;
	free_ent = ClearCode + 2;

	char_init();

	ent = GIFNextPixel();

	hshift = 0;
	for ( fcode = hsize; fcode < 65536; fcode *= 2 )
	    ++hshift;
	hshift = 8 - hshift;			// set hash code range bound

	hsize_reg = hsize;
	cl_hash( hsize_reg );	// clear hash table

	output( ClearCode, outs );

	outer_loop:
	while ( (c = GIFNextPixel()) != EOF )
	    {
	    fcode = ( c << maxbits ) + ent;
	    i = ( c << hshift ) ^ ent;		// xor hashing

	    if ( htab[i] == fcode )
		{
		ent = codetab[i];
		continue;
		}
	    else if ( htab[i] >= 0 )	// non-empty slot
		{
		disp = hsize_reg - i;	// secondary hash (after G. Knott)
		if ( i == 0 )
		    disp = 1;
		do
		    {
		    if ( (i -= disp) < 0 )
			i += hsize_reg;

		    if ( htab[i] == fcode )
			{
			ent = codetab[i];
			continue outer_loop;
			}
		    }
		while ( htab[i] >= 0 );
		}
	    output( ent, outs );
	    ent = c;
	    if ( free_ent < maxmaxcode )
		{
		codetab[i] = free_ent++;	// code -> hashtable
		htab[i] = fcode;
		}
	    else
		cl_block( outs );
	    }
	// Put out the final code.
	output( ent, outs );
	output( EOFCode, outs );
	}

    // output
    //
    // Output the given code.
    // Inputs:
    //      code:   A n_bits-bit integer.  If == -1, then EOF.  This assumes
    //              that n_bits =< wordsize - 1.
    // Outputs:
    //      Outputs code to the file.
    // Assumptions:
    //      Chars are 8 bits long.
    // Algorithm:
    //      Maintain a BITS character long buffer (so that 8 codes will
    // fit in it exactly).  Use the VAX insv instruction to insert each
    // code in turn.  When the buffer fills up empty it and start over.

    int cur_accum = 0;
    int cur_bits = 0;

    int masks[] = { 0x0000, 0x0001, 0x0003, 0x0007, 0x000F,
		    0x001F, 0x003F, 0x007F, 0x00FF,
		    0x01FF, 0x03FF, 0x07FF, 0x0FFF,
		    0x1FFF, 0x3FFF, 0x7FFF, 0xFFFF };

    void output( int code, OutputStream outs ) throws IOException
	{
	cur_accum &= masks[cur_bits];

	if ( cur_bits > 0 )
	    cur_accum |= ( code << cur_bits );
	else
	    cur_accum = code;

	cur_bits += n_bits;

	while ( cur_bits >= 8 )
	    {
	    char_out( (byte) ( cur_accum & 0xff ), outs );
	    cur_accum >>= 8;
	    cur_bits -= 8;
	    }

	// If the next entry is going to be too big for the code size,
	// then increase it, if possible.
       if ( free_ent > maxcode || clear_flg )
	    {
	    if ( clear_flg )
		{
		maxcode = MAXCODE(n_bits = g_init_bits);
		clear_flg = false;
		}
	    else
		{
		++n_bits;
		if ( n_bits == maxbits )
		    maxcode = maxmaxcode;
		else
		    maxcode = MAXCODE(n_bits);
		}
	    }

	if ( code == EOFCode )
	    {
	    // At EOF, write the rest of the buffer.
	    while ( cur_bits > 0 )
		{
		char_out( (byte) ( cur_accum & 0xff ), outs );
		cur_accum >>= 8;
		cur_bits -= 8;
		}

	    flush_char( outs );
	    }
	}

    // Clear out the hash table

    // table clear for block compress
    void cl_block( OutputStream outs ) throws IOException
	{
	cl_hash( hsize );
	free_ent = ClearCode + 2;
	clear_flg = true;

	output( ClearCode, outs );
	}

    // reset code table
    void cl_hash( int hsize )
	{
	for ( int i = 0; i < hsize; ++i )
	    htab[i] = -1;
	}

    // GIF Specific routines

    // Number of characters so far in this 'packet'
    int a_count;

    // Set up the 'byte output' routine
    void char_init()
	{
	a_count = 0;
	}

    // Define the storage for the packet accumulator
    byte[] accum = new byte[256];

    // Add a character to the end of the current packet, and if it is 254
    // characters, flush the packet to disk.
    void char_out( byte c, OutputStream outs ) throws IOException
	{
	accum[a_count++] = c;
	if ( a_count >= 254 )
	    flush_char( outs );
	}

    // Flush the packet to disk, and reset the accumulator
    void flush_char( OutputStream outs ) throws IOException
	{
	if ( a_count > 0 )
	    {
	    outs.write( a_count );
	    outs.write( accum, 0, a_count );
	    a_count = 0;
	    }
	}

    }

class GifEncoderHashitem
    {

    public int rgb;
    public int count;
    public byte index;

    public GifEncoderHashitem( int rgb, int count, byte index )
	{
	this.rgb = rgb;
	this.count = count;
	this.index = index;
	}

    }
