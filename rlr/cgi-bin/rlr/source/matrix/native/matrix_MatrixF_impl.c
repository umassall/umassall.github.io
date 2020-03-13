/****************************************************************/
/* Implementation of native code for class matrix.MatrixF,      */
/* filling in the body for each function                        */
/* defined as "extern" in "matrix_MatrixF.h", using the         */
/* same parameter names as in matrix.MatrixF.java.              */
/* Assumes "float" means 32-bit, and "double" is 64-bit.        */
/*                                                              */
/* This code is (c) 1997 Leemon Baird                           */
/* leemon@cs.cmu.edu                                            */
/* http://www.cs.cmu.edu/~baird                                 */
/* The source and object code may be redistributed freely.      */
/* If the code is modified, please state so in the comments.    */
/* version 1.0, 14 Apr 96                                       */
/****************************************************************/
//version 1.0 14 Apr 96 - major functions implemented, still need a few more
//
//written and checked:  
//     version, add2, diag1, mult2, multk, 
//     equalEls, add1, mult1, sub, subFrom, dot
//written but not fully checked:
//     multAdd, addMult, replace, diag2, multEl
//not written yet:
//     multColMat, multMatCol, multDiag
//
//differs from other matrix code in "MatrixF", "float"

#include <StubPreamble.h>
#include "matrix_MatrixF.h"
#include <stdio.h>
#define DEBUG

//loop through all elements in a matrix
#define FOR_ALL_ELEMENTS(_m,_c)                     \
   {                                                \
     long   rc        =_m->obj->restartCol;         \
     long   rr        =_m->obj->restartRow;         \
     long   newRow    =_m->obj->newRow;             \
     long   nextCol   =_m->obj->nextCol;            \
     float *endRow, *endCol;                       \
     _c               =_m->obj->data->obj->body +   \
                       _m->obj->first;              \
     for   (endRow=_c-rc; _c<endRow; _c+=newRow)    \
       for (endCol=_c-rr; _c<endCol; _c+=nextCol) {
#define NEXT_ALL_ELEMENTS }}

//loop through all corresponding elements in two matrices
#define FOR_SAME_ELEMENTS(_m1,_c1,_m2,_c2)            \
   {                                                  \
     long   rc        =_m1->obj->restartCol;          \
     long   rr        =_m1->obj->restartRow;          \
     long   newRow1   =_m1->obj->newRow;              \
     long   nextCol1  =_m1->obj->nextCol;             \
     long   newRow2   =_m2->obj->newRow;              \
     long   nextCol2  =_m2->obj->nextCol;             \
     float *endRow, *endCol;                         \
     _c1              =_m1->obj->data->obj->body +    \
                       _m1->obj->first;               \
     _c2              =_m2->obj->data->obj->body +    \
                       _m2->obj->first;               \
     for   (endRow=_c1-rc; _c1<endRow; _c1+=newRow1 ,_c2+=newRow2 )   \
       for (endCol=_c1-rr; _c1<endCol; _c1+=nextCol1,_c2+=nextCol2) {
#define NEXT_SAME_ELEMENTS }}

//Loop through elements in 3 matrices in the order used to calculate m1=m2 * m3.
//The code between the FOR and the first NEXT
//is executed once for each element of m1 (doing each element on the first
//row, then each element on the next row, etc).
//The code between the two NEXTs is executed once for each element in one
//row of m2 (which is also once for each element in one column of m3).
#define FOR_MULT_OUTER(_m1,_c1,_m2,_c2,_m3,_c3)     \
   {                                                \
     long   _rc1     =_m1->obj->restartCol;         \
     long   _rc2     =_m2->obj->restartCol;         \
     long   _rr1     =_m1->obj->restartRow;         \
     long   _rr2     =_m2->obj->restartRow;         \
     long   _newR1   =_m1->obj->newRow;             \
     long   _newR2   =_m2->obj->newRow;             \
     long   _newC2   =_m2->obj->newCol;             \
     long   _newC3   =_m3->obj->newCol;             \
     long   _nextC1  =_m1->obj->nextCol;            \
     long   _nextC2  =_m2->obj->nextCol;            \
     long   _nextR2  =_m2->obj->nextRow;            \
     long   _nextR3  =_m3->obj->nextRow;            \
     float *_endR1, *_endC1, *_endR2, *_c3s;       \
     _c1              =_m1->obj->data->obj->body +  \
                       _m1->obj->first;             \
     _c2              =_m2->obj->data->obj->body +  \
                       _m2->obj->first;             \
     _c3s             =_m3->obj->data->obj->body +  \
                       _m3->obj->first;             \
     _c3=_c3s;                                      \
     for     (_endR1=_c1-_rc1; _c1<_endR1; _c1+=_newR1 ,_c2+=_nextR2,_c3=_c3s)    {  \
       _endR2=_c2-_rr2;                                                              \
       for   (_endC1=_c1-_rr1; _c1<_endC1; _c1+=_nextC1,_c2+=_rr2,   _c3+=_newC3) {
#define FOR_MULT_INNER(_m1,_c1,_m2,_c2,_m3,_c3)                                      \
         for (; _c2<_endR2; _c2+=_nextC2,_c3+=_nextR3) {
#define NEXT_MULT_INNER }
#define NEXT_MULT_OUTER }}}


int64_t matrix_MatrixF_native_version
          (struct Hmatrix_MatrixF *t) {
  return 1; //version 1 (don't change this unless interface changes)
} //end version

long /*boolean*/ matrix_MatrixF_native_equalEls
          (struct Hmatrix_MatrixF *t,struct Hmatrix_MatrixF *this,
           struct Hmatrix_MatrixF *m) {
  float *thisCursor, *mCursor;

  FOR_SAME_ELEMENTS(this,thisCursor,m,mCursor)
    if (*thisCursor != *mCursor)
      return 0; //false if any element doesn't match
  NEXT_SAME_ELEMENTS;

  return 1; //true if all elements match
}//end equalEls

struct Hmatrix_MatrixF *matrix_MatrixF_native_add1
	  (struct Hmatrix_MatrixF *t,struct Hmatrix_MatrixF *this,
           struct Hmatrix_MatrixF *m) {
  float *thisCursor, *mCursor;

  FOR_SAME_ELEMENTS(this,thisCursor,m,mCursor)
    *thisCursor += *mCursor;
  NEXT_SAME_ELEMENTS;

  return this;
}//end add1

struct Hmatrix_MatrixF *matrix_MatrixF_native_sub
	  (struct Hmatrix_MatrixF *t,struct Hmatrix_MatrixF *this,
           struct Hmatrix_MatrixF *m) {
  float *thisCursor, *mCursor;

  FOR_SAME_ELEMENTS(this,thisCursor,m,mCursor)
    *thisCursor -= *mCursor;
  NEXT_SAME_ELEMENTS;

return this;
}//end sub



struct Hmatrix_MatrixF *matrix_MatrixF_native_subFrom
	  (struct Hmatrix_MatrixF *t,struct Hmatrix_MatrixF *this,
           struct Hmatrix_MatrixF *m) {
  float *thisCursor, *mCursor;

  FOR_SAME_ELEMENTS(this,thisCursor,m,mCursor)
    *thisCursor = *mCursor - *thisCursor;
  NEXT_SAME_ELEMENTS;

  return this;
}//end subFrom


struct Hmatrix_MatrixF *matrix_MatrixF_native_multAdd
	  (struct Hmatrix_MatrixF *t,struct Hmatrix_MatrixF *this,
           float k,
           struct Hmatrix_MatrixF *m) {
  float *thisCursor, *mCursor;

  FOR_SAME_ELEMENTS(this,thisCursor,m,mCursor)
    *thisCursor = *thisCursor * k + *mCursor;
  NEXT_SAME_ELEMENTS;

  return this;
}//end multadd


struct Hmatrix_MatrixF *matrix_MatrixF_native_addMult
	  (struct Hmatrix_MatrixF *t,struct Hmatrix_MatrixF *this,
           float k,
           struct Hmatrix_MatrixF *m) {
  float *thisCursor, *mCursor;

  FOR_SAME_ELEMENTS(this,thisCursor,m,mCursor)
    *thisCursor += *mCursor * k;
  NEXT_SAME_ELEMENTS;

  return this;
}// addmult


float matrix_MatrixF_native_dot
	  (struct Hmatrix_MatrixF *t,struct Hmatrix_MatrixF *this,
           struct Hmatrix_MatrixF *v) {
  float dot=0;
  float *vCursor   =   v->obj->data->obj->body +    
                        v->obj->first;
  float *thisCursor=this->obj->data->obj->body + 
                     this->obj->first;
  float *last      =this->obj->last + thisCursor;
  long   thisInc    =this->obj->next;
  long   vInc       =   v->obj->next; 
  
  //for all corresponding elements of two vectors, multiply them
  for (;thisCursor<=last; thisCursor+=thisInc, vCursor+=vInc) {
    dot += *thisCursor * *vCursor;
  }
  
  return dot; 
}//end dot


struct Hmatrix_MatrixF *matrix_MatrixF_native_replace
	  (struct Hmatrix_MatrixF *t,struct Hmatrix_MatrixF *this,
           struct Hmatrix_MatrixF *m) {
  float *thisCursor, *mCursor;

  FOR_SAME_ELEMENTS(this,thisCursor,m,mCursor)
    *thisCursor = *mCursor;
  NEXT_SAME_ELEMENTS;

  return this;
}//end replace


struct Hmatrix_MatrixF *matrix_MatrixF_native_add2
	  (struct Hmatrix_MatrixF *t,struct Hmatrix_MatrixF *this,
           float k) {
  float *cursor; //points to each element of the array

  FOR_ALL_ELEMENTS(this,cursor)
    *cursor += k;
  NEXT_ALL_ELEMENTS;

  return this;
}//end add2


struct Hmatrix_MatrixF *matrix_MatrixF_native_diag1
   (struct Hmatrix_MatrixF *t,struct Hmatrix_MatrixF *this,
           float k) {
  float *cursor =this->obj->data->obj->body +  //start of data array
                  this->obj->first;             //offset of start of this matrix
  float *last   =this->obj->last + cursor;     //lower-right corner of matrix
  long nextRowCol=this->obj->nextRow +          //go down one row ...
                  this->obj->nextCol;           //   and right one column

  matrix_MatrixF_native_mult2 (0,this,0);

  for (;cursor<last;cursor+=nextRowCol)
    *cursor=k;
  return this;
}//end diag1


struct Hmatrix_MatrixF *matrix_MatrixF_native_diag2
	  (struct Hmatrix_MatrixF *t,struct Hmatrix_MatrixF *this,
           struct Hmatrix_MatrixF *v) {
  float *cursor =this->obj->data->obj->body +  //start of data array
                  this->obj->first;             //offset of start of this matrix
  float *last   =this->obj->data->obj->body +
                  this->obj->last;              //lower-right corner of matrix
  long nextRowCol=this->obj->nextRow +          //go down one row ...
                  this->obj->nextCol;           //   and right one column
  float *vCursor=   v->obj->data->obj->body +    
                     v->obj->first;
  long   vInc    =   v->obj->next; 

  matrix_MatrixF_native_mult2 (0,this,0);

  for (;cursor<last;cursor+=nextRowCol, vCursor+=vInc)
    *cursor=*vCursor;
  return this;
}//end diag2


struct Hmatrix_MatrixF *matrix_MatrixF_native_mult1
	  (struct Hmatrix_MatrixF *t,struct Hmatrix_MatrixF *this,
           struct Hmatrix_MatrixF *x,
           struct Hmatrix_MatrixF *y) {
  float *te,*xe,*ye; //pointer to current element of this, x, and y respectively
  float dot=0; //dot product

  FOR_MULT_OUTER(this,te,x,xe,y,ye)      //for each element of this
    dot=0;
    FOR_MULT_INNER(this,te,x,xe,y,ye)    //for each element of one row of x, one col of y
      dot+=*xe * *ye;  
    NEXT_MULT_INNER                      //next element of the one row of x and col of y
    *te=dot;
  NEXT_MULT_OUTER;                       //next element of this

  return this;
}//end mult1


struct Hmatrix_MatrixF *matrix_MatrixF_native_mult2
	  (struct Hmatrix_MatrixF *t,struct Hmatrix_MatrixF *this,
           float k) {
  float *cursor; //points to each element of the array

  if (k==0) {
    FOR_ALL_ELEMENTS(this,cursor)
      *cursor = 0;
    NEXT_ALL_ELEMENTS;
  } else {
    FOR_ALL_ELEMENTS(this,cursor)
      *cursor *= k;
    NEXT_ALL_ELEMENTS;
  }

  return this;
}//end mult2


struct Hmatrix_MatrixF *matrix_MatrixF_native_multK
	  (struct Hmatrix_MatrixF *t,struct Hmatrix_MatrixF *this,
           float k,
           struct Hmatrix_MatrixF *m) {
  matrix_MatrixF_native_replace(t,this,m);
  matrix_MatrixF_native_mult2  (t,this,k);
  return this;
}//end multK


struct Hmatrix_MatrixF *matrix_MatrixF_native_multEl
	  (struct Hmatrix_MatrixF *t,struct Hmatrix_MatrixF *this,
           struct Hmatrix_MatrixF *m) {
  float *thisCursor, *mCursor;

  FOR_SAME_ELEMENTS(this,thisCursor,m,mCursor)
    *thisCursor *= *mCursor;
  NEXT_SAME_ELEMENTS(this,thisCursor,m,mCursor);

  return this;
}//end multEl


struct Hmatrix_MatrixF *matrix_MatrixF_native_multColMat
	  (struct Hmatrix_MatrixF *t,struct Hmatrix_MatrixF *this,
           struct Hmatrix_MatrixF *x,
           long   col,
           struct Hmatrix_MatrixF *v) {
  //    int xi,vi,ai; //index in array for x, y, answer
  //    int s1,s2,s3; //index at which each FOR loop should stop
  //
  //    if (nRows==x.nRows && 1==v.nRows && v.nCols==nCols && col>=0 && col<x.nCols) {
  //      xi=x.first + col*x.nextCol;
  //      s3=xi-x.restartRow;
  //      s1=first-restartCol;
  //      for (ai=first;ai<s1;ai+=newRow) {  //for each row of answer and x
  //        vi=v.first;
  //        s2=ai-restartRow;
  //        for (;ai<s2;ai+=nextCol) { //for each column of answer and y
  //          data[ai]=x.data[xi]*v.data[vi];
  //          vi+=v.nextCol;
  //        }
  //        xi+=x.nextRow;
  //      }
  //    } else //x, y, or this are wrong shape
  //      throw new MatrixException("error trying to multiply col x matrix "+
  //          nRows+"x"+  nCols+"="+x.nRows+"x"+x.nCols+"*"+v.nRows+"x"+v.nCols);
  //    return this; //return pointer to this object, which contains the answer
  #ifdef DEBUG 
  printf("Called native multColMat() \n");
  #endif
  return this;
}//end multColMat /**/


struct Hmatrix_MatrixF *matrix_MatrixF_native_multMatCol
	  (struct Hmatrix_MatrixF *t,struct Hmatrix_MatrixF *this,
           struct Hmatrix_MatrixF *x,
           struct Hmatrix_MatrixF *y,
           long   col) {
  //    int xi,yi,ai; //index in array for x, y, answer
  //    int s1,s3;    //index at which each FOR loop should stop
  //    float dot;    //dot product of one row of x and one column of y
  //
  //    if (nRows==x.nRows && x.nCols==y.nRows && col>=0 && col<y.nCols) {
  //      xi=x.first;
  //      s1=first-restartCol;
  //      for (ai=first;ai<s1;ai+=nextRow) {
  //        yi=y.first + col*y.nextCol;
  //        dot=0;
  //        s3=xi-x.restartRow;
  //        for (;xi<s3;xi+=x.nextCol) { // dot of the col'th column of y and a row of x
  //          dot+=x.data[xi]*y.data[yi];
  //          yi+=y.nextRow;
  //        }
  //        data[ai]=dot;
  //        xi+=x.newRow;
  //      }
  //    } else //x, y, or this are wrong shape
  //      throw new MatrixException("error trying to multiply Matrix x Col "+
  //          nRows+"x"+  nCols+"="+x.nRows+"x"+x.nCols+"*"+y.nRows+"x1\n");
  //    return this; //return pointer to this object, which contains the answer
  #ifdef DEBUG 
  printf("Called native multMatCol() \n");
  #endif
  return this;
}//end multMatCol /**/


struct Hmatrix_MatrixF *matrix_MatrixF_native_multDiag
	  (struct Hmatrix_MatrixF *t,struct Hmatrix_MatrixF *this,
           struct Hmatrix_MatrixF *x,
           struct Hmatrix_MatrixF *y) {
  //    int vi,yi,ai; //index in array for v, y, answer
  //    int s1,s2; //index at which each FOR loop should stop
  //
  //    if (nRows==v.size && nRows==y.nRows && nCols==y.nCols) {
  //      vi=v.first;
  //      yi=y.first;
  //      s1=first-restartCol;
  //      for (ai=first;ai<s1;ai+=newRow) { //for ai=start of each row of this
  //        s2=ai-restartRow;
  //        for (;ai<s2;ai+=nextCol) { //for ai=each col of this (within curr row)
  //            data[ai]=v.data[vi]*y.data[yi];
  //            yi+=y.nextCol;
  //        }
  //        vi+=v.next;
  //        yi+=y.newRow;
  //      }
  //    } else //v, y, or this are wrong shape
  //      throw new MatrixException("error trying to vector multiply matrices "+
  //          nRows+"x"+  nCols+"="+v.nRows+"x"+v.nCols+"x"+y.nRows+"x"+y.nCols);
  //    return this; //return pointer to this object, which contains the answer
  #ifdef DEBUG 
  printf("Called native multDiag() \n");
  #endif
  return this;
} //end multDiag /**/
