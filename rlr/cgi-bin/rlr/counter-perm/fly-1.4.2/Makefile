#This makefile should suffice on most Unix systems. For other systems,
#you may well need to rewrite it. 
#
# If you already have gd installed, use the line below and comment out the
# following line
#       GD = /path/to/gd/directory
        GD = gd1.2
        CC = gcc
    CFLAGS = -g -w -I$(GD)
      LIBS = -L$(GD) -lgd -lm

all: gd fly

gd: $(GD)
	cd $(GD); make

fly.o: fly.c fly.h
	$(CC) -c $(CFLAGS) fly.c

fly: fly.o
	$(CC) -o fly fly.o $(LIBS)

mostlyclean:
	rm -f fly.o temp.gif core; cd gd1.2; make clean;

clean:
	rm -f fly.o fly temp.gif core; cd gd1.2; make clean;

