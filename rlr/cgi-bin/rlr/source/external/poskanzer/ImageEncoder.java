// ImageEncoder - abstract class for writing out an image
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

package external.poskanzer;

import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.image.*;

/// Abstract class for writing out an image.
// <P>
// A framework for classes that encode and write out an image in
// a particular file format.
// <P>
// <A HREF="/resources/classes/Acme/ImageEncoder.java">Fetch the software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.Z">Fetch the entire Acme package.</A>
// <P>
// @see GifEncoder
// @see PpmEncoder

public abstract class ImageEncoder implements ImageConsumer
    {

    OutputStream out;

    private ImageProducer prod;
    private int width = -1;
    private int height = -1;
    private int hintflags = 0;
    private boolean started = false;
    private boolean encoding;
    private IOException iox;
    private static ColorModel rgbModel = ColorModel.getRGBdefault();

    /// Constructor.
    // @param img The image to encode.
    // @param out The stream to write the bytes to.
    public ImageEncoder( Image img, OutputStream out )
	{
	this( img.getSource(), out );
	}

    /// Constructor.
    // @param prod The ImageProducer to encode.
    // @param out The stream to write the bytes to.
    public ImageEncoder( ImageProducer prod, OutputStream out )
	{
	this.prod = prod;
	this.out = out;
	}

    // Here are the methods that subclasses implement:

    /// Subclasses implement this to initialize an encoding.
    abstract void encodeStart( int w, int h ) throws IOException;

    /// Subclasses implement this to actually write out some bits.  They
    // are guaranteed to be delivered in top-bottom-left-right order.
    abstract void encodeRgb(
	int x, int y, int w, int h, int[] rgbPixels, int off, int scansize )
	throws IOException;

    /// Subclasses implement this to finish an encoding.
    abstract void encodeDone() throws IOException;


    /// Called to get things going.
    public synchronized void encode() throws IOException
	{
	encoding = true;
	iox = null;
	prod.startProduction( this );
	while ( encoding )
	    try
		{
		wait();
		}
	    catch ( InterruptedException e ) {}
	if ( iox != null )
	    throw iox;
	}

    public void setDimensions( int width, int height )
	{
	this.width = width;
	this.height = height;
	}

    public void setProperties( Hashtable props )
	{
	// Ignore.
	}

    public void setColorModel( ColorModel model )
	{
	// Ignore.
	}

    public void setHints( int hintflags )
	{
	this.hintflags = hintflags;
	}

    public void setPixels(
	int x, int y, int w, int h, ColorModel model, byte[] pixels,
	int off, int scansize )
	{
	int[] rgbPixels = new int[w];
	for ( int row = 0; row < h; ++row )
	    {
	    int rowOff = off + row * scansize;
	    for ( int col = 0; col < w; ++col )
		{
		int i = rowOff + col;
		rgbPixels[col] = model.getRGB( pixels[i] & 0xff );
		}
	    try
		{
		encodeRgbWrapper( x, y + row, w, 1, rgbPixels, 0, w );
		}
	    catch ( IOException e )
		{
		iox = e;
		done();
		return;
		}
	    }
	}

    public void setPixels(
	int x, int y, int w, int h, ColorModel model, int[] pixels,
	int off, int scansize )
	{
	if ( model == rgbModel )
	    try
		{
		encodeRgbWrapper( x, y, w, h, pixels, off, scansize );
		}
	    catch ( IOException e )
		{
		iox = e;
		done();
		return;
		}
	else
	    {
	    int[] rgbPixels = new int[w];
            for ( int row = 0; row < h; ++row )
		{
		int rowOff = off + row * scansize;
                for ( int col = 0; col < w; ++col )
		    {
		    int i = rowOff + col;
                    rgbPixels[col] = model.getRGB( pixels[i] );
		    }
		try
		    {
		    encodeRgbWrapper( x, y + row, w, 1, rgbPixels, 0, w );
		    }
		catch ( IOException e )
		    {
		    iox = e;
		    done();
		    return;
		    }
		}
	    }
	}

    private void encodeRgbWrapper(
	int x, int y, int w, int h, int[] rgbPixels, int off, int scansize )
	throws IOException
	{
	if ( ! started )
	    {
	    started = true;
	    encodeStart( width, height );
	    if ( ( hintflags & TOPDOWNLEFTRIGHT ) == 0 )
		{
		prod.requestTopDownLeftRightResend( this );
		return;
		}
	    }
	encodeRgb( x, y, w, h, rgbPixels, off, scansize );
	}

    public void imageComplete( int status )
	{
	prod.removeConsumer( this );
	if ( ( status & ImageConsumer.IMAGEABORTED ) != 0 )
	    iox = new IOException( "image aborted" );
	else
	    {
	    try
		{
		encodeDone();
		}
	    catch ( IOException e )
		{
		iox = e;
		}
	    }
	done();
	}

    private synchronized void done()
	{
	encoding = false;
	notifyAll();
	}

    }
