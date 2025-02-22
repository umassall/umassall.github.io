/* DO NOT EDIT THIS FILE - it is machine generated */
#include <native.h>
/* Header for class matrix_MatrixD */

#ifndef _Included_matrix_MatrixD
#define _Included_matrix_MatrixD
struct Hexpression_NumExp;

typedef struct Classmatrix_MatrixD {
/* Inaccessible static: nativeCode */
    struct HArrayOfDouble *data;
    long nRows;
    long nCols;
    long nextRow;
    long nextCol;
    long first;
    long last;
    long newRow;
    long newCol;
    long restartRow;
    long restartCol;
    long size;
    long next;
    struct HArrayOfObject *expression;
} Classmatrix_MatrixD;
HandleTo(matrix_MatrixD);

#ifdef __cplusplus
extern "C" {
#endif
extern int64_t matrix_MatrixD_native_version(struct Hmatrix_MatrixD *);
struct Hmatrix_MatrixD;
extern /*boolean*/ long matrix_MatrixD_native_equalEls(struct Hmatrix_MatrixD *,struct Hmatrix_MatrixD *,struct Hmatrix_MatrixD *);
extern struct Hmatrix_MatrixD *matrix_MatrixD_native_add1(struct Hmatrix_MatrixD *,struct Hmatrix_MatrixD *,struct Hmatrix_MatrixD *);
extern struct Hmatrix_MatrixD *matrix_MatrixD_native_sub(struct Hmatrix_MatrixD *,struct Hmatrix_MatrixD *,struct Hmatrix_MatrixD *);
extern struct Hmatrix_MatrixD *matrix_MatrixD_native_subFrom(struct Hmatrix_MatrixD *,struct Hmatrix_MatrixD *,struct Hmatrix_MatrixD *);
extern struct Hmatrix_MatrixD *matrix_MatrixD_native_multAdd(struct Hmatrix_MatrixD *,struct Hmatrix_MatrixD *,double,struct Hmatrix_MatrixD *);
extern struct Hmatrix_MatrixD *matrix_MatrixD_native_addMult(struct Hmatrix_MatrixD *,struct Hmatrix_MatrixD *,double,struct Hmatrix_MatrixD *);
extern double matrix_MatrixD_native_dot(struct Hmatrix_MatrixD *,struct Hmatrix_MatrixD *,struct Hmatrix_MatrixD *);
extern struct Hmatrix_MatrixD *matrix_MatrixD_native_replace(struct Hmatrix_MatrixD *,struct Hmatrix_MatrixD *,struct Hmatrix_MatrixD *);
extern struct Hmatrix_MatrixD *matrix_MatrixD_native_add2(struct Hmatrix_MatrixD *,struct Hmatrix_MatrixD *,double);
extern struct Hmatrix_MatrixD *matrix_MatrixD_native_diag1(struct Hmatrix_MatrixD *,struct Hmatrix_MatrixD *,double);
extern struct Hmatrix_MatrixD *matrix_MatrixD_native_diag2(struct Hmatrix_MatrixD *,struct Hmatrix_MatrixD *,struct Hmatrix_MatrixD *);
extern struct Hmatrix_MatrixD *matrix_MatrixD_native_mult1(struct Hmatrix_MatrixD *,struct Hmatrix_MatrixD *,struct Hmatrix_MatrixD *,struct Hmatrix_MatrixD *);
extern struct Hmatrix_MatrixD *matrix_MatrixD_native_mult2(struct Hmatrix_MatrixD *,struct Hmatrix_MatrixD *,double);
extern struct Hmatrix_MatrixD *matrix_MatrixD_native_multK(struct Hmatrix_MatrixD *,struct Hmatrix_MatrixD *,double,struct Hmatrix_MatrixD *);
extern struct Hmatrix_MatrixD *matrix_MatrixD_native_multEl(struct Hmatrix_MatrixD *,struct Hmatrix_MatrixD *,struct Hmatrix_MatrixD *);
extern struct Hmatrix_MatrixD *matrix_MatrixD_native_multColMat(struct Hmatrix_MatrixD *,struct Hmatrix_MatrixD *,struct Hmatrix_MatrixD *,long,struct Hmatrix_MatrixD *);
extern struct Hmatrix_MatrixD *matrix_MatrixD_native_multMatCol(struct Hmatrix_MatrixD *,struct Hmatrix_MatrixD *,struct Hmatrix_MatrixD *,struct Hmatrix_MatrixD *,long);
extern struct Hmatrix_MatrixD *matrix_MatrixD_native_multDiag(struct Hmatrix_MatrixD *,struct Hmatrix_MatrixD *,struct Hmatrix_MatrixD *,struct Hmatrix_MatrixD *);
#ifdef __cplusplus
}
#endif
#endif
