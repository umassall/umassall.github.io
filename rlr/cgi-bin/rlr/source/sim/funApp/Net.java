package sim.funApp;
import sim.funApp.FunApp;
import parse.*;
import matrix.*;
import expression.*;
import java.util.*;
import pointer.*;

/** Net is a general Neural Network architecture which computes the first and second
  * derivitives wrt the weights and inputs.  The structure is specified using keywords
  * which describe the weighting scheme from one layer to the next and the activation
  * function used within a layer.  When necessary a number of nodes is also specified in
  * parenthesis following the activation function keyword.  To use Net you should use the
  * following when a FunApp is required:           Net {a w a(#) w a(#) w a(#) w a}
  * 'a' is chosen from the following set of activation functions, {Identity,
  * HardlimitingSquashing, Bipolar, Monopolar, ExponentialInverted, HyperbolicTan, Sin, Gaussian}.
  * 'w' is chosen from the following set of weighting schemes, {Linear, Quadratic1, Quadratic2}.
  * '#' stands for number of nodes in that layer.  Note that activation functions
  * must be in the first and last positions and these two actiavtion functions should not
  * specifiy a number of nodes because that information is given in the Data object.
  *
  * Original MLP was written by Mance E. Harmon and has been enhanced by Scott Weaver
  *    <p>This code is (c) 1997 Scott E. Weaver
  *    <<a href=mailto:scott.weaver@uc.edu>scott.weaver@uc.edu</a>>,
  *    <a href=http://http://www.ececs.uc.edu/~sweaver>
  *    http://http://www.ececs.uc.edu/~sweaver</a><br>
  *    The source and object code may be redistributed freely provided
  *    no fee is charged.  If the code is modified, please state so
  *    in the comments.
  * @author Scott Weaver
  * @author Leemon Baird
  * @version 1.01, 10 Jun 97  added Quadratic2 for SISO RBFs (Bug in FindHessian for Quadratic2)
  *
  * Becuase this code is so convoluted that the results should and can and should be easily
  * checked using Mathematica to take the first and second derivitives of the error wrt the
  * weights and the inputs.  Places where the code may be inefficient I've noted with SLOW.
  *
  * The variables have a specific convention.  dSikdwik means derivative of the single node Sik
  * with respect to the vector wik.  The derivitive of this variable with respect to Simo would
  * be dSikdwikdSimo. imo means the i minus one'th layer. ik means the ith layer and the kth node
  * in layer i.  A variable that ends in T stands for Transpose.  'u' refers to weights in all
  * layers following (but not including) the current layer. 'w' refers to weights in the current
  * layer.  Variables of the form d?*
  * stands for derrivitive of ? wrt *, where ? and * are usually variables defined elsewhere
  * Comments with a number such as (4) or 5 or |2| refer to my personal notes - Scott Weaver
  * The folowing variables are used so frequently that have been shortened in varios places
  * prev=nN[i-1].val is Number of nodes in previous layer
  * curr=nN[i].val   is Number of nodes in current  layer
  */
public class Net extends FunApp {
//  @version 1.0,  4  May 97
//  @version 1.01, 10 Jun 97  added Quadratic2 for SISO RBFs (bug in FindHessian for Quadratic2)
  private IntExp[] nN;                 //list of number of nodes in layer (input/hidden/output)
  private IntExp[] weightType;         //list of possible weight types
  private IntExp[] activationType;     //list of possible activation functions
  private IntExp[] noWtsIntoS;         //number of weights into a node Sik
  private int      numLayers=0;        //the number of layers including output but not input

  private MatrixD B[][];               // B[i][k] is square weight matrix for Quadratic
  private MatrixD BT[][];
  private MatrixD C[][];               // C[i][k] is vector weight matrix for Quadratic
  private MatrixD AmoC[][];            // Amo-C
  private MatrixD AmoCT[][];
  private MatrixD BmultAmoC[][];         // B . Amoc  (used in Quadratic2)
  private MatrixD dSikdwikdSimo[][];
  private MatrixD dSikdwikdwik[][];

  private MatrixD dSikdBdBj[][][];
  private MatrixD dSikdBikdCikj[][][];
  private MatrixD dSikdCikjdBik[][][];
  private MatrixD dSikdCikdCik[][];

  private MatrixD dSikdBikdSimo[][];
  private MatrixD dSikdSimodSimo[][];
  private MatrixD dSikdBikdSimoj[][][];
  private MatrixD dSikdCikdSimo[][];
  private MatrixD W[];                  // used with Linear weighting scheme
  private MatrixD WT[];
  private MatrixD S[];                  // S[i] holds output of weighting scheme
  private MatrixD A[];                  // A[i] holds output of activation function
  private MatrixD Ap[];                 // Derrivitive[S[i],i]
  private MatrixD ApT[];
  private MatrixD App[];                // Derrivitive[S[i],i,2] (second derivite)
  private MatrixD AppT[];
  private MatrixD AT[];
  private MatrixD dEdS[];
  private MatrixD dSdSmo[];
  private MatrixD dSikdSmo[][];
  private MatrixD dEdu[];
  private MatrixD dEdwik[][];           // will be submatrixs from larger dEdu[]
  private MatrixD dSikdwik[][];
  private MatrixD dSikdwikT[][];
  private MatrixD dSikdCik[][];
  private MatrixD dSikdBik[][];
  private MatrixD dSikdAimo[][];

/* required for hessain */
  private MatrixD dSdSmoT[];
  private MatrixD dEdudS[];         //(3)
  private MatrixD dEdudSmo[];       //|5|
  private MatrixD dEdwikdSmo[][];   //|4| // subvectors of dEdudS[]
  private MatrixD dEdSdS[];         //(4) // n[i] * n[i]
  private MatrixD dSdSmodSmo[];     // 5
  private MatrixD dEdudu[];         //(5)
  private MatrixD dEdwikdwij[][][]; //|3| // subvectors of dEdudu[]
  private MatrixD dEdudwik[][];     //|2| // subvectors of dEdudu[]
  private MatrixD dEdwikdu[][];     //|2| // subvectors of dEdudu[]

/* temporary storage variables.  name indicates dimension. ni is number of nodes in current
 * layer, nimo is number of nodes in previous layer. should improve by finding largest
 * MatrixD object needed and then overlaping all of these temporary storages */
  private MatrixD niXnimo[];
  private MatrixD niXni[];
  private MatrixD niXniT[];
  private MatrixD nimoXnimo[];
  private MatrixD niXniAGAIN[];
  private MatrixD nimoXnimoT[];
  private MatrixD nimoXNoWtsIntoS[];
  private MatrixD nimoXNoWtsIntoST[];
  private MatrixD NoWtsIntoSXNoWtsIntoS[];
  private MatrixD niXone[];
  private MatrixD nimoXone[];
  private MatrixD oneXnimo[];
  private MatrixD nimoXni[];

  /** Define the MatrixD objects that will be used by evaluate()
    * and findGradients().  All 9 should be column vectors (n by 1 matrices).
    * All the MatrixD objects are copied, but the pointers still point
    * to the same data arrays.
    * @exception MatrixException if vector shapes don't match
    */
  public void setIO(MatrixD inVect, MatrixD outVect, MatrixD weights,
                    MatrixD dEdIn,  MatrixD dEdOut,  MatrixD dEdWeights,
                    MatrixD dEdIndIn, MatrixD dEdOutdOut, MatrixD dEdWeightsdWeights)
                 throws MatrixException {
    super.setIO(inVect, outVect, weights, dEdIn, dEdOut, dEdWeights, dEdIndIn, dEdOutdOut, dEdWeightsdWeights);

    B                  = new MatrixD[numLayers+1][];
    C                  = new MatrixD[numLayers+1][];
    BT                 = new MatrixD[numLayers+1][];
    AmoC               = new MatrixD[numLayers+1][];
    AmoCT              = new MatrixD[numLayers+1][];
    BmultAmoC          = new MatrixD[numLayers+1][];
    dSikdwikdSimo      = new MatrixD[numLayers+1][];
    dSikdwikdwik       = new MatrixD[numLayers+1][];
    dSikdBikdCikj      = new MatrixD[numLayers+1][][];
    dSikdBdBj          = new MatrixD[numLayers+1][][];
    dSikdCikjdBik      = new MatrixD[numLayers+1][][];
    dSikdCikdCik       = new MatrixD[numLayers+1][];
    dSikdCikdSimo      = new MatrixD[numLayers+1][];
    dSikdBikdSimo      = new MatrixD[numLayers+1][];
    dSikdSimodSimo     = new MatrixD[numLayers+1][];
    dSikdBikdSimoj     = new MatrixD[numLayers+1][][];
    W                  = new MatrixD[numLayers+1];
    WT                 = new MatrixD[numLayers+1];
    S                  = new MatrixD[numLayers+1];
    A                  = new MatrixD[numLayers+1];
    Ap                 = new MatrixD[numLayers+1];
    ApT                = new MatrixD[numLayers+1];
    App                = new MatrixD[numLayers+1];
    AppT               = new MatrixD[numLayers+1];
    AT                 = new MatrixD[numLayers+1];
    dEdS               = new MatrixD[numLayers+1];
    dSdSmo             = new MatrixD[numLayers+1];
    dSikdSmo           = new MatrixD[numLayers+1][];
    dEdu               = new MatrixD[numLayers+1];
    dEdwik             = new MatrixD[numLayers+1][]; // will be submatrix of dEdu
    dSikdwik           = new MatrixD[numLayers+1][];
    dSikdwikT          = new MatrixD[numLayers+1][];
    dSikdCik           = new MatrixD[numLayers+1][];
    dSikdBik           = new MatrixD[numLayers+1][];
    dSikdAimo          = new MatrixD[numLayers+1][];

/* required for hessain */
    dEdSdS                  = new MatrixD[numLayers+1];
    dSdSmoT                 = new MatrixD[numLayers+1];
    dSdSmodSmo              = new MatrixD[numLayers+1];
    dEdudu                  = new MatrixD[numLayers+1];
    dEdudS                  = new MatrixD[numLayers+1];
    dEdwikdwij              = new MatrixD[numLayers+1][][];// will be a submatrix of dEdudu
    dEdwikdSmo              = new MatrixD[numLayers+1][];  // will be part of dEdudS
    dEdudSmo                = new MatrixD[numLayers+1];    // will be part of dEdudS
    dEdudwik                = new MatrixD[numLayers+1][];
    dEdwikdu                = new MatrixD[numLayers+1][];  // Transpose of above SLOW

/* temporary storage variables */
    niXone                  = new MatrixD[numLayers+1];
    nimoXone                = new MatrixD[numLayers+1];
    oneXnimo                = new MatrixD[numLayers+1];
    niXnimo                 = new MatrixD[numLayers+1];
    niXni                   = new MatrixD[numLayers+1];
    niXniT                  = new MatrixD[numLayers+1];
    nimoXnimo               = new MatrixD[numLayers+1];
    niXniAGAIN              = new MatrixD[numLayers+1];
    nimoXnimoT              = new MatrixD[numLayers+1];
    nimoXNoWtsIntoS         = new MatrixD[numLayers+1];
    nimoXNoWtsIntoST        = new MatrixD[numLayers+1];
    NoWtsIntoSXNoWtsIntoS   = new MatrixD[numLayers+1];
    nimoXni                 = new MatrixD[numLayers+1];

    nN[0].val=inVect.size;                                // couldn't do this in parse
    nN[numLayers].val=outVect.size;                       // couldn't do this in parse

    for (int i=0; i<=numLayers; i++) {
        int curr =nN[i].val;
        int prev =nN[i-1].val;
        S[i]    =new MatrixD(curr);
        A[i]    =new MatrixD(curr);
        Ap[i]   =new MatrixD(curr);
        ApT[i]  =new MatrixD(curr);
        App[i]  =new MatrixD(curr);
        AppT[i] =new MatrixD(curr);
        AT[i]   =new MatrixD(curr);
        dEdS[i] =new MatrixD(curr);
    }

/* interface shared variables with ones internal to this object */
    S[0]=(MatrixD)inVect.clone();                      // attach S[0] to inVect
    if (dEdWeights!=null)
      dEdu[0]   = (MatrixD)dEdWeights.clone();         // dEdu[0] has an element for each weight
    if (dEdWeightsdWeights!=null)
      dEdudu[0] = (MatrixD)dEdWeightsdWeights.clone(); // dEdudu[0] will be the hessian
    A[numLayers]= (MatrixD)outVect.clone();            // attach A[numHLayers+1] to outVect

    if (dEdOut!=null) {
        dEdS[numLayers]=(MatrixD)dEdOut.clone();       // at this stage, its actually dEdA
    }
    if (dEdIn!=null) {
        dEdS[0]=(MatrixD)dEdIn.clone();
    }
    if (dEdOutdOut!=null) {
        dEdSdS[numLayers]  = (MatrixD) dEdOutdOut.clone();  // at this stage, its actually dEdAdA
    }
    if (dEdIndIn!=null) {
        dEdSdS[0]  = (MatrixD) dEdIndIn.clone();
    }

    for (int i=0; i<=numLayers; i++) {
        AT[i]       =((MatrixD)      A[i].clone()).transpose();         // link to A
        ApT[i]      =((MatrixD)     Ap[i].clone()).transpose();         // link to Ap
        AppT[i]     =((MatrixD)    App[i].clone()).transpose();         // link to App
    }

    for (int i=1; i<=numLayers; i++) {
        int curr =nN[i].val;
        int prev =nN[i-1].val;
        switch(weightType[i-1].val) {
            default:
            case 0: // Linear
                noWtsIntoS[i] = new IntExp(prev);
            break;
            case 1: // Quadratic1 with     full B  (prev*prev) matrix and center C (prev*1)
            case 2: // Quadratic2 with (2) full B  (prev*prev) matrix and center C (prev*1)
                noWtsIntoS[i] = new IntExp(prev*(1+prev));
            break;
            case 3: // Diagonal - Quadratic with diagonal B (in*1) and center (in*1)
            break;
        }
        niXone[i]                = new MatrixD(curr);
        nimoXone[i]              = new MatrixD(prev);
        oneXnimo[i]              = new MatrixD(1,prev);
        niXnimo[i]               = new MatrixD(curr,prev);
        niXni[i]                 = new MatrixD(curr,curr);
        niXniT[i]                = ((MatrixD)niXni[i].clone()).transpose();
        niXniAGAIN[i]            = new MatrixD(curr,curr);
        nimoXnimo[i]             = new MatrixD(prev,prev);
        nimoXnimoT[i]            = ((MatrixD)nimoXnimo[i].clone()).transpose();
        nimoXNoWtsIntoS[i]       = new MatrixD(prev, noWtsIntoS[i].val);//used with Quadratic
        nimoXNoWtsIntoST[i]      = ((MatrixD)nimoXNoWtsIntoS[i].clone()).transpose();
        NoWtsIntoSXNoWtsIntoS[i] = new MatrixD(noWtsIntoS[i].val,noWtsIntoS[i].val);
        dSdSmodSmo[i]            = new MatrixD(prev,prev);
    }
    for (int i=1,wAcum=0; i<=numLayers; i++) {
        int curr =nN[i].val;
        int prev =nN[i-1].val;
        switch(weightType[i-1].val) {
            default:
            case 0: // Linear
                noWtsIntoS[i] = new IntExp(prev);                    // SLOW can be deleted
                W[i] =((MatrixD)weights.clone()).submatrix(wAcum,curr,prev);
                WT[i]=((MatrixD)W[i].clone()).transpose();
            break;
            case 1: // Quadratic1
            case 2: // Quadratic2
                noWtsIntoS[i] = new IntExp(prev*(1+prev));           // SLOW can be deleted
                B[i]          = new MatrixD[curr];
                BT[i]         = new MatrixD[curr];
                C[i]          = new MatrixD[curr];
                AmoC[i]       = new MatrixD[curr];
                AmoCT[i]      = new MatrixD[curr];
                BmultAmoC[i]  = new MatrixD[curr];

                for(int k=0,ind=0;k<curr;k++) {
                    C[i][k]        = ((MatrixD)weights.clone()).submatrix(wAcum+ind,prev,1);
                    BmultAmoC[i][k]= new MatrixD(prev,1);
                    AmoC[i][k]     = new MatrixD(prev,1);
                    AmoCT[i][k]    = ((MatrixD)AmoC[i][k].clone()).transpose();
                    ind+=prev;
                    B[i][k] = ((MatrixD)weights.clone()).submatrix(wAcum+ind,prev,prev);
                    BT[i][k]= ((MatrixD)B[i][k].clone()).transpose();
                    ind+=prev*prev;
                }
            break;
            case 3: // Diagonal
            break;
        }           // end switch
        dSdSmo[i]             = new MatrixD(prev,curr);             // why not done earlier ??
        dSikdSmo[i]           = new MatrixD[curr];                  // why not done earlier ??
        for(int k=0;k<curr;k++) {
            dSikdSmo[i][k]   = ((MatrixD)dSdSmo[i].clone()).submatrix(0,k,prev,1);
        }
        dSdSmoT[i]= ((MatrixD)dSdSmo[i].clone()).transpose();
        int wSize=curr * noWtsIntoS[i].val;
    if (dEdu[i-1]!=null)
        dEdu[i]  =((MatrixD)dEdu[i-1].  clone()).submatrix(wSize,dEdu[i-1].nRows-wSize,1);
    if (dEdudu[i-1]!=null)
/*(5)*/ dEdudu[i]=((MatrixD)dEdudu[i-1].clone()).submatrix(wSize,wSize,dEdudu[i-1].nRows-wSize,dEdudu[i-1].nCols-wSize);
        wAcum += wSize;
    }

    for (int i=1; i<=numLayers; i++) {
        int curr =nN[i].val;
        int prev =nN[i-1].val;
        dSikdwikdSimo[i]     = new MatrixD[curr];
        dSikdwikdwik[i]      = new MatrixD[curr];
        dSikdSimodSimo[i]    = new MatrixD[curr];
        for(int k=0;k<curr;k++) {
            dSikdwikdSimo[i][k]  = new MatrixD(prev,noWtsIntoS[i].val);//dSikdwikdSimo[i][k] contains two smaller maticies
            dSikdSimodSimo[i][k] = new MatrixD(prev,prev);                                             // 5
        }  // used by both Linear and Quadratic in |4|
        switch(weightType[i-1].val) { // only make weights needed for this, the ith, layer
            default:
            case 0: // Linear
            break;
            case 1: // Quadratic1 with     full B (in*in) matrix and center (in*1)
            case 2: // Quadratic2 with (2) full B (in*in) matrix and center (in*1)
//            noWtsIntoS[i]        = new IntExp(prev*(1+prev));

            dSikdBikdCikj[i]      = new MatrixD[curr][];
            dSikdBdBj[i]          = new MatrixD[curr][];
            dSikdCikjdBik[i]      = new MatrixD[curr][];
            dSikdCikdCik[i]       = new MatrixD[curr];

            dSikdCikdSimo[i]     = new MatrixD[curr];
            dSikdBikdSimo[i]     = new MatrixD[curr];
            dSikdBikdSimoj[i]    = new MatrixD[curr][];
            for(int k=0;k<curr;k++) {
                dSikdwikdwik[i][k]   = new MatrixD(noWtsIntoS[i].val,noWtsIntoS[i].val);//dSikdwikdwik[i][k] contains: BwithB, CwC, BwC, CwB
                dSikdCikdSimo[i][k]  = ((MatrixD)dSikdwikdSimo[i][k].clone()).submatrix(0,0,prev,prev);
                dSikdBikdSimo[i][k]  = ((MatrixD)dSikdwikdSimo[i][k].clone()).submatrix(0,prev,prev,prev*prev);
                // previous two lines form the nimo by (nimo+nimo*nimo) matrix dSikdwikdSimo[i][k]

                dSikdBikdSimoj[i][k] = new MatrixD[prev];
                dSikdBikdCikj[i][k]  = new MatrixD[prev];
                dSikdBdBj[i][k]      = new MatrixD[prev];
                dSikdCikjdBik[i][k]  = new MatrixD[prev];
                dSikdCikdCik[i][k]   = ((MatrixD)dSikdwikdwik[i][k].clone()).submatrix(0,0,prev,prev);
  /* Sik is a scalar, Cikj is the jth element of the Cik vector, therefore dSikdCikjdBik has the same
  * dimension as Bik (prev x prev).  The use of submatrix below pulls a vector from the matrix dSikdwikdwik
  * and puts it into a (prev x prev) matrix.
  */
                for(int j=0;j<prev;j++) {
dSikdBdBj[i][k][j]     =((MatrixD)dSikdwikdwik [i][k].clone()).submatrix((j+1)*prev,(j+1)*prev,prev,prev);

dSikdBikdSimoj[i][k][j]=((MatrixD)dSikdBikdSimo[i][k].clone()).submatrix(j,0,1,prev*prev).submatrix   (0,prev,prev);
dSikdBikdCikj[i][k][j] =((MatrixD)dSikdwikdwik [i][k].clone()).submatrix(prev,j,prev*prev,1).submatrix(0,prev,prev);
dSikdCikjdBik[i][k][j] =((MatrixD)dSikdwikdwik [i][k].clone()).submatrix(j,prev,1,prev*prev).submatrix(0,prev,prev);
                }
            }
            break;
            case 3: // Diagonal
            break;
        }  // end switch
    }  // end for

    for (int i=1; i<numLayers; i++) {
        dEdSdS[i]     = new MatrixD(nN[i].val,nN[i].val);
    }
    dEdudS[numLayers] = new MatrixD(0,nN[numLayers].val); // sets aside space for dEdudS[i]
    for (int i=numLayers, wAcum=0; i>0; i--) {
        wAcum+=nN[i].val*noWtsIntoS[i].val;
        dEdudS[i-1] = new MatrixD(wAcum,nN[i-1].val);  /* (3) */
    }
    for(int i=numLayers-1,wAcum=0;i>0;i--) {    // new code
        int ind = nN[i].val * noWtsIntoS[i].val;
        wAcum += noWtsIntoS[i+1].val * nN[i+1].val;
        dEdudSmo[i] = ((MatrixD) dEdudS[i-1].clone()).submatrix(ind,0,wAcum,nN[i-1].val);
    }
    for (int i=1; i<=numLayers; i++) {
        dSikdwik[i]   =  new MatrixD[nN[i].val];
        dSikdwikT[i]  =  new MatrixD[nN[i].val];
        for(int k=0;k<nN[i].val;k++) {
            dSikdwik[i][k]= new MatrixD(noWtsIntoS[i].val);
            dSikdwikT[i][k]= ((MatrixD)dSikdwik[i][k].clone()).transpose();
        }
        switch(weightType[i-1].val) {
            default:
            case 0: // L
                for(int k=0;k<nN[i].val;k++) {
                   dSikdwik[i][k]=(MatrixD)A[i-1].clone();
                   dSikdwikT[i][k]=(MatrixD)AT[i-1].clone();
//                 niXniAGAIN[i].diag(Ap[i]);  don't know why this code is here
//                 dSikdwikdSimo[i][k]=(MatrixD)Ap[i-1].clone(); decided to divide Net from NN w/ |4|
                }
            break;
            case 1: // Quadratic1
            case 2: // Quadratic2
                dSikdCik[i]   =  new MatrixD[nN[i].val];
                dSikdBik[i]   =  new MatrixD[nN[i].val];
                dSikdAimo[i]  =  new MatrixD[nN[i].val];
                for(int k=0;k<nN[i].val;k++) {
                    dSikdCik[i][k]=((MatrixD)dSikdwik[i][k].clone()).submatrix(0,nN[i-1].val,1);
                    dSikdBik[i][k]=((MatrixD)dSikdwik[i][k].clone()).submatrix(nN[i-1].val,nN[i-1].val,nN[i-1].val);
                    dSikdAimo[i][k]=new MatrixD(nN[i-1].val);
                }
            break; //
            case 3:  // Diagonal
            break; //
            }// end switch
    }
    for (int i=1; i<=numLayers; i++) {
          dEdwik[i]   =  new MatrixD[nN[i].val]; // nN[i].val
          for(int k=0,ind=0;k<nN[i].val;k++) {
            if(dEdu[i-1]!=null)
              dEdwik[i][k]= ((MatrixD)dEdu[i-1].clone()).submatrix(ind,noWtsIntoS[i].val,1);
            ind+=noWtsIntoS[i].val;   // ind holds how far to ind into dEdu
          }
      }
    for (int i=1; i<=numLayers; i++) {
          dEdwikdSmo[i] =  new MatrixD[nN[i].val];
    }
    for (int i=1; i<=numLayers; i++) {
          for(int k=0,ind=0;k<nN[i].val;k++) {
            dEdwikdSmo[i][k]= ((MatrixD)dEdudS[i-1].clone()).submatrix(ind,0,noWtsIntoS[i].val,nN[i-1].val);
            // this is matrix, rows=no weights, cols=no nodes inprev layer, no of matrixes = no nodes in this layer
            ind+=noWtsIntoS[i].val;   // ind holds how far to ind into previous dEdu[]
          }
    }
    for (int i=1; i<=numLayers; i++) {
          dEdwikdwij[i]    =  new MatrixD[nN[i].val][nN[i].val]; // make n[i]^2
          dEdudwik[i]      =  new MatrixD[nN[i].val];  //      make n[i] of these
          dEdwikdu[i]      =  new MatrixD[nN[i].val];  //      make n[i] of these
          for(int k=0,ind=0;k<nN[i].val;k++) {
            for(int j=0,ind2=0;j<nN[i].val;j++) {
              if(dEdudu[i-1] != null) {
  /*(5)*/       dEdwikdwij[i][k][j] = ((MatrixD)dEdudu[i-1].clone()).submatrix(ind2,ind,noWtsIntoS[i].val,noWtsIntoS[i].val);
                ind2+=noWtsIntoS[i].val;
              }
            }
            int wSize     = nN[i].val*noWtsIntoS[i].val;              //  is # of weights in this layer
            if (dEdudu[i-1]!=null) {
    /*(5)*/   dEdwikdu[i][k] = ((MatrixD)dEdudu[i-1].clone()).submatrix(ind,wSize,noWtsIntoS[i].val,dEdudu[i-1].nCols-wSize);
    /*(5)*/   dEdudwik[i][k] = ((MatrixD)dEdudu[i-1].clone()).submatrix(wSize,ind,dEdudu[i-1].nRows-wSize,noWtsIntoS[i].val);
            }
            ind+=noWtsIntoS[i].val;   // ind holds how far to ind into dEdu
          }
    }
    if (dEdIn!=null && inVect.size!=dEdIn.size)
      throw new MatrixException("mismatched sizes: input vector "+
                        inVect.size+", input gradient "+dEdIn.size);
    if (dEdOut!=null && outVect.size!=dEdOut.size)
      throw new MatrixException("mismatched sizes: output vector "+
                        outVect.size+", output gradient "+dEdOut.size);
    if (dEdWeights!=null && weights.size!=dEdWeights.size)
      throw new MatrixException("mismatched sizes: weights "+
                        weights.size+", weights gradient "+dEdWeights.size);

    if (dEdIndIn!=null && (inVect.size!=dEdIndIn.nRows || inVect.size!=dEdIndIn.nCols))
      throw new MatrixException("mismatched sizes: input vector "+
                        inVect.size+", input gradient "+dEdIndIn.size);

    if (dEdOutdOut!=null && (outVect.size!=dEdOutdOut.nRows || outVect.size!=dEdOutdOut.nCols))
      throw new MatrixException("mismatched sizes: output vector "+
                        outVect.size+", output gradient "+dEdOutdOut.size);

    if (dEdWeightsdWeights!=null && (weights.size!=dEdWeightsdWeights.nRows || weights.size!=dEdWeightsdWeights.nCols))
      throw new MatrixException("mismatched sizes: weights "+
                        weights.size+", weights gradient "+dEdWeightsdWeights.size);
  }//end setIO

  /** calculate the output for the given input.  Also calculates Ap and App because
  * it is appropriate to do so eventhough they are not used until backward pass */
  public void evaluate() {
    try {
        for (int j=0,i=0; j<nN[i].val; j++) {// must have at least one activation type
            A[i].set  (j,activation           ( S[i].val(j),             activationType[i].val));
            Ap[i].set (j,activationPrime      ( S[i].val(j),A[i].val(j), activationType[i].val));
            App[i].set(j,activationDoublePrime( S[i].val(j),A[i].val(j), activationType[i].val));
        }
        for (int i=1, wAcum=0; i<=numLayers; i++) {
            switch(weightType[i-1].val) {
                default:
                case 0: // L
                    S[i].mult(W[i],A[i-1]);
                break;
                case 1:  // Quadratic1
                    for(int k=0,ind=0;k<nN[i].val;k++) {
                        AmoC[i][k].replace(A[i-1]);
                        AmoC[i][k].sub(C[i][k]);
                        nimoXone[i].mult(B[i][k],AmoC[i][k]);
                        S[i].set(k,AmoCT[i][k].dot(nimoXone[i]));
                    }
                break;
                case 2:  // Quadratic2
                    for(int k=0,ind=0;k<nN[i].val;k++) {
                        AmoC[i][k].replace(A[i-1]);
                        AmoC[i][k].sub(C[i][k]);
                        BmultAmoC[i][k].mult(B[i][k],AmoC[i][k]);
                        S[i].set(k,BmultAmoC[i][k].dot(BmultAmoC[i][k]));  // CHECK THIS OUTPUT !!!!
                    }
                break;
                case 3:
                break;
            }  // end switch
            for (int j=0; j<nN[i].val; j++) {
                A[i].set  (j,activation           ( S[i].val(j),             activationType[i].val));
                Ap[i].set (j,activationPrime      ( S[i].val(j),A[i].val(j), activationType[i].val));
                App[i].set(j,activationDoublePrime( S[i].val(j),A[i].val(j), activationType[i].val));
            } // end for
//        System.out.println("A["+i+"]  \n"+A[i]);
        }// end for
    }  // end try
    catch (MatrixException e) {
        e.print();
    }
  }  //end evaluate

  /** Calculate the output, gradient, for a given input.
    * This does everything evaluate() does, plus
    * it calculates the Gradient of the error with resepect to the
    * the weights and inputs, dEdx, and dEdw.
    * User must set dEdOut and dEdOut before calling.
    */
  public void findGradients() {
  try {
  evaluate();
  for (int i=numLayers; i>0; i--) {
        if(i==numLayers) {                        // last activation layer backpropagation
            // when dEdS, dEdSdS are first filled they are actually dEdA dEdAdA this if block corrects that
//            niXniAGAIN[i].diag(Ap[i]);                                                        // |6| b
//            niXni[i].mult(dEdSdS[i],niXniAGAIN[i]); // niXni is diagonal so T is not necessary// |6| b
//            dEdSdS[i].mult(niXniAGAIN[i],niXni[i]);                                           // |6| b
//            niXone[i].replace(App[i]);                                                        // |6| a
//            niXone[i].multEl(dEdS[i]);   // at this stage dEdS[i] is really dEdA[i]           // |6| a
//            niXni[i].diag(niXone[i]);                                                         // |6| a
//            dEdSdS[i].add(niXni[i]);                                                          // |6|
            dEdS[i].multEl(Ap[i]); // after this line, dEdS is now correctly named,
        }
        for (int k=0; k<nN[i].val; k++) {
          switch(weightType[i-1].val) {
            default:
            case 0: //
                dSdSmo[i].multDiag(Ap[i-1],WT[i]);                                                 //  2
            break;
            case 1: // Quadratic1 with form vBv
                dSikdCikdCik[i][k].replace(B[i][k]);                                               //  3
                dSikdCikdCik[i][k].add(BT[i][k]);       // partially filling dSikdwikdwik[i][k]    //  3
                dSikdAimo[i][k].mult(dSikdCikdCik[i][k],AmoC[i][k]);                               //  2
                dSikdSmo[i][k].multDiag(Ap[i-1],dSikdAimo[i][k]);                                  //  2
                     // for the last layer maybe the above, dSikdAimo, could be unity, maybe not!

                dSikdCik[i][k].replace(dSikdAimo[i][k]);                                           //  1
                dSikdCik[i][k].mult(-1);                                                           //  1
                dSikdBik[i][k].mult(AmoC[i][k],AmoCT[i][k]);         // fills dSikdwik             //  1

                //  dSikdBik has dimension nimoXnimo, dSikdBikdSimo adds a third dimension of size
                //  nimo indexed by j, therefore dSikdBikdSimoj is again a matrix of dim nimoXnimo
                //  We fill in individual values of this matrix using p as an index for the both the
                //  jth row and jth column to fill in AmoC(p).
                //  in dSikdBikdCikj, j indexes the element in Cik (similar to above)
//                for(int j=0;j<nN[i-1].val;j++) {
//                    for(int p=0;p<nN[i-1].val;p++) {
//                        if(p==j) {  // put the 2, why? since D[x^2,x]=2 x
//                    dSikdBikdSimoj[i][k][j].set(j,p,2*AmoC[i][k].val(p)*Ap[i-1].val(j));           //  4
//                    dSikdBikdCikj[i][k][j].set(j,p,2*AmoC[i][k].val(p)*(-1));                      //  3
//                    dSikdCikjdBik[i][k][j].set(j,p,2*AmoC[i][k].val(p)*(-1));                      //  3
//                        } else {
//                    dSikdBikdSimoj[i][k][j].set(j,p,AmoC[i][k].val(p)*Ap[i-1].val(j));             //  4
//                    dSikdBikdSimoj[i][k][j].set(p,j,AmoC[i][k].val(p)*Ap[i-1].val(j));             //  4
//                    dSikdBikdCikj[i][k][j].set(j,p,AmoC[i][k].val(p)*(-1));                        //  3
//                    dSikdBikdCikj[i][k][j].set(p,j,AmoC[i][k].val(p)*(-1));                        //  3
//                    dSikdCikjdBik[i][k][j].set(j,p,AmoC[i][k].val(p)*(-1));                        //  3
//                    dSikdCikjdBik[i][k][j].set(p,j,AmoC[i][k].val(p)*(-1));//dSikdwikdwik,done     //  3
//                        }
//                    }
//                }
//                dSikdCikdSimo[i][k].multDiag(Ap[i-1],dSikdCikdCik[i][k]);                                //  4
//                dSikdCikdSimo[i][k].mult(-1);                        // fills in dSikdwikdSimo     //  4
//
//                dSikdSimodSimo[i][k].mult(Ap[i-1],ApT[i-1]);                                       //  5
//                dSikdSimodSimo[i][k].multEl(dSikdCikdCik[i][k]);//dSikdCikdCik=dSikdAimodAimo      //  5
//                for (int j=0; j<nN[i-1].val; j++) {                                                //  5
//                    dSikdSimodSimo[i][k].set(j,j,dSikdSimodSimo[i][k].val(j,j) + dSikdAimo[i][k].val(j)*App[i-1].val(j)); //  5
//                }                                                                                  //  5
            break;
            case 2:  // Quadratic2 with form vBBv
                dSikdCikdCik[i][k].mult(BT[i][k],B[i][k]);                                         //  3
                dSikdCikdCik[i][k].mult(2);       // partially filling dSikdwikdwik[i][k]          //  3
                dSikdAimo[i][k].mult(dSikdCikdCik[i][k],AmoC[i][k]);                               //  2
                dSikdSmo[i][k].multDiag(Ap[i-1],dSikdAimo[i][k]);                                  //  2
                     // for the last layer maybe the above, dSikdAimo, could be unity, maybe not!

                dSikdCik[i][k].replace(dSikdAimo[i][k]);                                           //  1
                dSikdCik[i][k].mult(-1);                                                           //  1
                dSikdBik[i][k].mult(BmultAmoC[i][k],AmoCT[i][k]);                                        //  1
                dSikdBik[i][k].mult(2);                             // fills dSikdwik              //  1

                //  dSikdBik has dimension nimoXnimo, dSikdBikdSimo adds a third dimension of size
                //  nimo indexed by j, therefore dSikdBikdSimoj is again a matrix of dim nimoXnimo
                //  We fill in individual values of this matrix using p as an index for the both the
                //  jth row and jth column to fill in AmoC(p).
                //  in dSikdBikdCikj, j indexes the element in Cik (similar to above)
//                double scalar;
//                nimoXnimo[i].mult(AmoC[i][k],AmoCT[i][k]);
//                nimoXnimo[i].mult(2.0);
//                for(int j=0;j<nN[i-1].val;j++) {
//                    dSikdBdBj[i][k][j].replace(nimoXnimo[i]); // fills to make diagonal block                                //  3
//                }
//                for(int j=0;j<nN[i-1].val;j++) {
//                    for(int p1=0;p1<nN[i-1].val;p1++) {
//                        scalar= 2.0 * B[i][k].submatrix(p1,0,1,nN[i-1].val).dot(AmoC[i][k]);
//                        for(int p2=0;p2<nN[i-1].val;p2++) {
//                            dSikdBikdSimoj[i][k][j].set(p1,p2, 2.0 * AmoC[i][k].val(p2)*B[i][k].val(p1,j)*Ap[i-1].val(j)); //  3
//                            dSikdBikdCikj[i][k][j] .set(p1,p2,-2.0 * AmoC[i][k].val(p2)*B[i][k].val(p1,j)               ); //  3
//                            dSikdCikjdBik[i][k][j] .set(p1,p2,-2.0 * AmoC[i][k].val(p2)*B[i][k].val(p1,j)); //need?//  3
//                            if(j==p2) {
//                            dSikdBikdSimoj[i][k][j].set(p1,p2,dSikdBikdCikj[i][k][j].val(p1,p2)+scalar*Ap[i-1].val(j)); // add scalar
//                            dSikdBikdCikj[i][k][j] .set(p1,p2,dSikdBikdCikj[i][k][j].val(p1,p2)-scalar);
//                            dSikdCikjdBik[i][k][j] .set(p1,p2,dSikdCikjdBik[i][k][j].val(p1,p2)-scalar);
//                            // the minus sign in each of the two previous lines is because Aimo-Cik=AmoC
//                            }
//                        }
//                    }
//                }
//                dSikdCikdSimo[i][k].multDiag(Ap[i-1],dSikdCikdCik[i][k]);                          //  4
//                dSikdCikdSimo[i][k].mult(-1);                        // fills in dSikdwikdSimo     //  4
//
//                dSikdSimodSimo[i][k].mult(Ap[i-1],ApT[i-1]);                                       //  5
//                dSikdSimodSimo[i][k].multEl(dSikdCikdCik[i][k]);//dSikdCikdCik=dSikdAimodAimo      //  5
//                for (int j=0; j<nN[i-1].val; j++) {                                                //  5
//                    dSikdSimodSimo[i][k].set(j,j,dSikdSimodSimo[i][k].val(j,j) + dSikdAimo[i][k].val(j)*App[i-1].val(j)); //  5
//                }                                                                                  //  5
            break; //
            case 3:     //diagonal B quadratic not yet implemented
            break; //
          }  // end switch
          dEdwik[i][k].multK(dEdS[i].val(k),dSikdwik[i][k]);                                       // |1|
        }
//        switch(weightType[i-1].val) {
//            default:
//            case 0: // L
//            for (int k=0; k<nN[i].val; k++) {                                                  // |3|
//                for (int j=0; j<nN[i].val; j++) {                                              // |3|
//                    NoWtsIntoSXNoWtsIntoS[i].mult(dSikdwik[i][j],dSikdwikT[i][k]);             // |3| b                                  // |3|
//                    dEdwikdwij[i][k][j].replace(NoWtsIntoSXNoWtsIntoS[i]);                     // |3| b
//                    dEdwikdwij[i][k][j].mult(dEdSdS[i].val(k,j));                              // |3| b
//                }   // |3| a not needed since dSikdwikdwik = 0 for linear                      // |3|
//            }                                                                                  // |3|
//            for (int k=0; k<nN[i].val; k++) {                                                  // |4| b
//                nimoXone[i].multMatCol(dSdSmo[i],dEdSdS[i],k);                                 // |4| b
//                dEdwikdSmo[i][k].transpose();// can transpose now since |4| a is sysm             |4| b
//                dEdwikdSmo[i][k].mult(nimoXone[i],AT[i-1]);                                    // |4| b
//                dEdwikdSmo[i][k].transpose();// now return to original                            |4| b
//            }                                                                                 //  |4| b
//            for (int k=0; k<nN[i].val; k++) {                                                 //  |4| a
//                nimoXone[i].replace(Ap[i-1]);// in theory really a n[i-1] X n[i-1] mat            |4| a
//                nimoXone[i].mult(dEdS[i].val(k)); // still a diag matrix                          |4| a
//                for (int j=0; j<nN[i-1].val; j++) {        //loop includs only diag entries       |4| a
//                   dEdwikdSmo[i][k].set(j,j,dEdwikdSmo[i][k].val(j,j)+nimoXone[i].val(j));     // |4| a
//                }                                                                              // |4| a
//            }                                                                                  // |4| a
//            break;
//            case 1: // Quadratic1
//            case 2: // Quadratic2
//            for (int k=0; k<nN[i].val; k++) {                                                  // |3|
//                for (int j=0; j<nN[i].val; j++) {                                              // |3|
//                    NoWtsIntoSXNoWtsIntoS[i].mult(dSikdwik[i][j],dSikdwikT[i][k]);             // |3| b                                  // |3|
//                    dEdwikdwij[i][k][j].replace(NoWtsIntoSXNoWtsIntoS[i]);                     // |3| b
//                    dEdwikdwij[i][k][j].mult(dEdSdS[i].val(k,j));                              // |3| b
//                }                                                                              // |3|
//                NoWtsIntoSXNoWtsIntoS[i].multK(dEdS[i].val(k),dSikdwikdwik[i][k]);             // |3| a
//                dEdwikdwij[i][k][k].add(NoWtsIntoSXNoWtsIntoS[i]);  // add this part if k = k  // |3| a
//            }
//
//            for (int k=0; k<nN[i].val; k++) {                                                  // |4| b
//                nimoXone[i].multMatCol(dSdSmo[i],dEdSdS[i],k);                                 // |4| b
//                dEdwikdSmo[i][k].transpose();// can transpose now since |4| a is sysm             |4| b
//                dEdwikdSmo[i][k].mult(nimoXone[i],dSikdwikT[i][k]);                            // |4| b
//                dEdwikdSmo[i][k].transpose();// now return to original                            |4| b
//            }                                                                                  // |4| b
//            for (int k=0; k<nN[i].val; k++) {                                                  // |4| a
//                nimoXNoWtsIntoS[i].replace(dSikdwikdSimo[i][k]);                               // |4| a
//                nimoXNoWtsIntoS[i].mult(dEdS[i].val(k));                                       // |4| a
//                dEdwikdSmo[i][k].add(nimoXNoWtsIntoST[i]);                                     // |4| a
//            }
//            break;
//        }// end switch
//        if(i<numLayers) { // if i=numLayers there "u" is empty set
//            for (int k=0; k<nN[i].val; k++) {                                                  // |2|
//                dEdudwik[i][k].multColMat(dEdudS[i],k,dSikdwikT[i][k]);                        // |2|
//                dEdudwik[i][k].transpose();                                                    // |2|
//                dEdwikdu[i][k].replace(dEdudwik[i][k]);                                        // |2|
//                dEdudwik[i][k].transpose();                                                    // |2|
//            }                                                                                  // |2|
//            dEdudSmo[i].mult(dEdudS[i],dSdSmoT[i]);                                            // |5|
//        }
        dEdS[i-1].mult(dSdSmo[i],dEdS[i]);                                                     // (2)

        nimoXnimo[i].mult(0);
        switch(weightType[i-1].val) { // only make weights needed for this, the ith, layer
            default:
            case 0: // Linear
                nimoXone[i].mult(WT[i],dEdS[i]);                                               // (4) a
                nimoXone[i].multEl(App[i-1]);                                                  // (4) a
                nimoXnimo[i].diag(nimoXone[i]);                                                // (4) a
            break;
            case 1: // Quadratic1 with     full B (in*in) matrix and center (in*1)
            case 2: // Quadratic2 with (2) full B (in*in) matrix and center (in*1)
                for (int k=0; k<nN[i].val; k++) {                                              // (4) a
                    nimoXnimo[i].addMult(dEdS[i].val(k),dSikdSimodSimo[i][k]);                 // (4) a
                }                                                                              // (4) a
            break;
            case 3: // Quadratic with diagonal B (in*1) and center (in*1)
            break;
        }
        ///**/ I've wrapped the following in an if/then so it will stop crashing
        ///**/ WebSim.  But why is this code in findGradients() to begin with?
        ///**/ If it's a second derivative calculation, why isn't it in
        ///**/ findHessian where it belongs?
        if (dEdSdS!=null && dEdSdS[i]!=null && dSdSmoT!=null && dSdSmoT[i]!=null/**/
            && dEdSdS[i-1]!=null && dSdSmo!=null && dSdSmo[i]!=null             /**/
            && niXnimo!=null && niXnimo[i]!=null) {                             /**/
          niXnimo[i].mult(dEdSdS[i],dSdSmoT[i]);                                                 // (4) b
          dEdSdS[i-1].mult(dSdSmo[i],niXnimo[i]);                                                // (4) b
          dEdSdS[i-1].add(nimoXnimo[i]);                                                         // (4) b
        }/**/
  } // end for
      } catch (MatrixException e) {
          e.print();
      }
//      for (int i=numLayers; i>=0; i--) {
//          System.out.println("  dEdSdS["+i+"]  \n"+dEdSdS[i]);
//          System.out.println("  dEdu["+i+"]  \n"+dEdu[i]);
//          System.out.println("  dEdudu["+i+"]  \n"+dEdudu[i]);
//      }
//      try {
//         System.out.println("  dEdudu[0]  \n"+dEdudu[0].submatrix(0,0,1,dEdudu[0].nCols));
//         // (0,0,1 .. is 0th weight, C10 in this case
//      } catch (MatrixException e) {
//          e.print();
//      }
  }//end findGradients

  /** Calculate the output, gradient, and Hessian for a given input.
    * This does everything evaluate() and findGradients() do, plus
    * it calculates the Hessian of the error with resepect to the
    * the weights and inputs, dEdxdx, dEdwdx, and dEdwdw.
    * User must set dEdOut and dEdOutdOut before calling.
    */
  public void findHessian() {
  try {
  evaluate();
  for (int i=numLayers; i>0; i--) {
        if(i==numLayers) {                        // last activation layer backpropagation
            // when dEdS, dEdSdS are first filled they are actually dEdA dEdAdA this if block corrects that
            niXniAGAIN[i].diag(Ap[i]);                                                        // |6| b
            niXni[i].mult(dEdSdS[i],niXniAGAIN[i]); // niXni is diagonal so T is not necessary// |6| b
            dEdSdS[i].mult(niXniAGAIN[i],niXni[i]);                                           // |6| b
            niXone[i].replace(App[i]);                                                        // |6| a
            niXone[i].multEl(dEdS[i]);   // at this stage dEdS[i] is really dEdA[i]           // |6| a
            niXni[i].diag(niXone[i]);                                                         // |6| a
            dEdSdS[i].add(niXni[i]);                                                          // |6|
            dEdS[i].multEl(Ap[i]); // after this line, dEdS is now correctly named,
        }
        for (int k=0; k<nN[i].val; k++) {
          switch(weightType[i-1].val) {
            default:
            case 0: //
                dSdSmo[i].multDiag(Ap[i-1],WT[i]);                                                 //  2
            break;
            case 1: // Quadratic1 with form vBv
                dSikdCikdCik[i][k].replace(B[i][k]);                                               //  3
                dSikdCikdCik[i][k].add(BT[i][k]);       // partially filling dSikdwikdwik[i][k]    //  3
                dSikdAimo[i][k].mult(dSikdCikdCik[i][k],AmoC[i][k]);                               //  2
                dSikdSmo[i][k].multDiag(Ap[i-1],dSikdAimo[i][k]);                                  //  2
                     // for the last layer maybe the above, dSikdAimo, could be unity, maybe not!

                dSikdCik[i][k].replace(dSikdAimo[i][k]);                                           //  1
                dSikdCik[i][k].mult(-1);                                                           //  1
                dSikdBik[i][k].mult(AmoC[i][k],AmoCT[i][k]);         // fills dSikdwik             //  1

                //  dSikdBik has dimension nimoXnimo, dSikdBikdSimo adds a third dimension of size
                //  nimo indexed by j, therefore dSikdBikdSimoj is again a matrix of dim nimoXnimo
                //  We fill in individual values of this matrix using p as an index for the both the
                //  jth row and jth column to fill in AmoC(p).
                //  in dSikdBikdCikj, j indexes the element in Cik (similar to above)
                for(int j=0;j<nN[i-1].val;j++) {
                    for(int p=0;p<nN[i-1].val;p++) {
                        if(p==j) {  // put the 2, why? since D[x^2,x]=2 x
                    dSikdBikdSimoj[i][k][j].set(j,p,2*AmoC[i][k].val(p)*Ap[i-1].val(j));           //  4
                    dSikdBikdCikj[i][k][j].set(j,p,2*AmoC[i][k].val(p)*(-1));                      //  3
                    dSikdCikjdBik[i][k][j].set(j,p,2*AmoC[i][k].val(p)*(-1));                      //  3
                        } else {
                    dSikdBikdSimoj[i][k][j].set(j,p,AmoC[i][k].val(p)*Ap[i-1].val(j));             //  4
                    dSikdBikdSimoj[i][k][j].set(p,j,AmoC[i][k].val(p)*Ap[i-1].val(j));             //  4
                    dSikdBikdCikj[i][k][j].set(j,p,AmoC[i][k].val(p)*(-1));                        //  3
                    dSikdBikdCikj[i][k][j].set(p,j,AmoC[i][k].val(p)*(-1));                        //  3
                    dSikdCikjdBik[i][k][j].set(j,p,AmoC[i][k].val(p)*(-1));                        //  3
                    dSikdCikjdBik[i][k][j].set(p,j,AmoC[i][k].val(p)*(-1));//dSikdwikdwik,done     //  3
                        }
                    }
                }
                dSikdCikdSimo[i][k].multDiag(Ap[i-1],dSikdCikdCik[i][k]);                                //  4
                dSikdCikdSimo[i][k].mult(-1);                        // fills in dSikdwikdSimo     //  4

                dSikdSimodSimo[i][k].mult(Ap[i-1],ApT[i-1]);                                       //  5
                dSikdSimodSimo[i][k].multEl(dSikdCikdCik[i][k]);//dSikdCikdCik=dSikdAimodAimo      //  5
                for (int j=0; j<nN[i-1].val; j++) {                                                //  5
                    dSikdSimodSimo[i][k].set(j,j,dSikdSimodSimo[i][k].val(j,j) + dSikdAimo[i][k].val(j)*App[i-1].val(j)); //  5
                }                                                                                  //  5
            break;
            case 2:  // Quadratic2 with form vBBv
System.out.println("There is a bug in findHessian, Quadratic2 \n");
                dSikdCikdCik[i][k].mult(BT[i][k],B[i][k]);                                         //  3
                dSikdCikdCik[i][k].mult(2);       // partially filling dSikdwikdwik[i][k]          //  3
                dSikdAimo[i][k].mult(dSikdCikdCik[i][k],AmoC[i][k]);                               //  2
                dSikdSmo[i][k].multDiag(Ap[i-1],dSikdAimo[i][k]);                                  //  2
                     // for the last layer maybe the above, dSikdAimo, could be unity, maybe not!

                dSikdCik[i][k].replace(dSikdAimo[i][k]);                                           //  1
                dSikdCik[i][k].mult(-1);                                                           //  1
                dSikdBik[i][k].mult(BmultAmoC[i][k],AmoCT[i][k]);                                        //  1
                dSikdBik[i][k].mult(2);                             // fills dSikdwik              //  1

                //  dSikdBik has dimension nimoXnimo, dSikdBikdSimo adds a third dimension of size
                //  nimo indexed by j, therefore dSikdBikdSimoj is again a matrix of dim nimoXnimo
                //  We fill in individual values of this matrix using p as an index for the both the
                //  jth row and jth column to fill in AmoC(p).
                //  in dSikdBikdCikj, j indexes the element in Cik (similar to above)
                double scalar;
                nimoXnimo[i].mult(AmoC[i][k],AmoCT[i][k]);
                nimoXnimo[i].mult(2.0);
                for(int j=0;j<nN[i-1].val;j++) {
                    dSikdBdBj[i][k][j].replace(nimoXnimo[i]); // fills to make diagonal block                                //  3
                }
                for(int j=0;j<nN[i-1].val;j++) {
                    for(int p1=0;p1<nN[i-1].val;p1++) {
                        scalar= 2.0 * B[i][k].submatrix(p1,0,1,nN[i-1].val).dot(AmoC[i][k]);
                        for(int p2=0;p2<nN[i-1].val;p2++) {
                            dSikdBikdSimoj[i][k][j].set(p1,p2, 2.0 * AmoC[i][k].val(p2)*B[i][k].val(p1,j)*Ap[i-1].val(j)); //  3
                            dSikdBikdCikj[i][k][j] .set(p1,p2,-2.0 * AmoC[i][k].val(p2)*B[i][k].val(p1,j)               ); //  3
                            dSikdCikjdBik[i][k][j] .set(p1,p2,-2.0 * AmoC[i][k].val(p2)*B[i][k].val(p1,j)); //need?//  3
                            if(j==p2) {
                            dSikdBikdSimoj[i][k][j].set(p1,p2,dSikdBikdCikj[i][k][j].val(p1,p2)+scalar*Ap[i-1].val(j)); // add scalar
                            dSikdBikdCikj[i][k][j] .set(p1,p2,dSikdBikdCikj[i][k][j].val(p1,p2)-scalar);
                            dSikdCikjdBik[i][k][j] .set(p1,p2,dSikdCikjdBik[i][k][j].val(p1,p2)-scalar);
                            // the minus sign in each of the two previous lines is because Aimo-Cik=AmoC
                            }
                        }
                    }
                }
                dSikdCikdSimo[i][k].multDiag(Ap[i-1],dSikdCikdCik[i][k]);                          //  4
                dSikdCikdSimo[i][k].mult(-1);                        // fills in dSikdwikdSimo     //  4

                dSikdSimodSimo[i][k].mult(Ap[i-1],ApT[i-1]);                                       //  5
                dSikdSimodSimo[i][k].multEl(dSikdCikdCik[i][k]);//dSikdCikdCik=dSikdAimodAimo      //  5
                for (int j=0; j<nN[i-1].val; j++) {                                                //  5
                    dSikdSimodSimo[i][k].set(j,j,dSikdSimodSimo[i][k].val(j,j) + dSikdAimo[i][k].val(j)*App[i-1].val(j)); //  5
                }                                                                                  //  5
            break; //
            case 3:     //diagonal B quadratic not yet implemented
            break; //
          }  // end switch
          dEdwik[i][k].multK(dEdS[i].val(k),dSikdwik[i][k]);                                       // |1|
        }
        switch(weightType[i-1].val) {
            default:
            case 0: // L
            for (int k=0; k<nN[i].val; k++) {                                                  // |3|
                for (int j=0; j<nN[i].val; j++) {                                              // |3|
                    NoWtsIntoSXNoWtsIntoS[i].mult(dSikdwik[i][j],dSikdwikT[i][k]);             // |3| b                                  // |3|
                    dEdwikdwij[i][k][j].replace(NoWtsIntoSXNoWtsIntoS[i]);                     // |3| b
                    dEdwikdwij[i][k][j].mult(dEdSdS[i].val(k,j));                              // |3| b
                }   // |3| a not needed since dSikdwikdwik = 0 for linear                      // |3|
            }                                                                                  // |3|
            for (int k=0; k<nN[i].val; k++) {                                                  // |4| b
                nimoXone[i].multMatCol(dSdSmo[i],dEdSdS[i],k);                                 // |4| b
                dEdwikdSmo[i][k].transpose();// can transpose now since |4| a is sysm             |4| b
                dEdwikdSmo[i][k].mult(nimoXone[i],AT[i-1]);                                    // |4| b
                dEdwikdSmo[i][k].transpose();// now return to original                            |4| b
            }                                                                                 //  |4| b
            for (int k=0; k<nN[i].val; k++) {                                                 //  |4| a
                nimoXone[i].replace(Ap[i-1]);// in theory really a n[i-1] X n[i-1] mat            |4| a
                nimoXone[i].mult(dEdS[i].val(k)); // still a diag matrix                          |4| a
                for (int j=0; j<nN[i-1].val; j++) {        //loop includs only diag entries       |4| a
                   dEdwikdSmo[i][k].set(j,j,dEdwikdSmo[i][k].val(j,j)+nimoXone[i].val(j));     // |4| a
                }                                                                              // |4| a
            }                                                                                  // |4| a
            break;
            case 1: // Quadratic1
            case 2: // Quadratic2
            for (int k=0; k<nN[i].val; k++) {                                                  // |3|
                for (int j=0; j<nN[i].val; j++) {                                              // |3|
                    NoWtsIntoSXNoWtsIntoS[i].mult(dSikdwik[i][j],dSikdwikT[i][k]);             // |3| b                                  // |3|
                    dEdwikdwij[i][k][j].replace(NoWtsIntoSXNoWtsIntoS[i]);                     // |3| b
                    dEdwikdwij[i][k][j].mult(dEdSdS[i].val(k,j));                              // |3| b
                }                                                                              // |3|
                NoWtsIntoSXNoWtsIntoS[i].multK(dEdS[i].val(k),dSikdwikdwik[i][k]);             // |3| a
                dEdwikdwij[i][k][k].add(NoWtsIntoSXNoWtsIntoS[i]);  // add this part if k = k  // |3| a
            }

            for (int k=0; k<nN[i].val; k++) {                                                  // |4| b
                nimoXone[i].multMatCol(dSdSmo[i],dEdSdS[i],k);                                 // |4| b
                dEdwikdSmo[i][k].transpose();// can transpose now since |4| a is sysm             |4| b
                dEdwikdSmo[i][k].mult(nimoXone[i],dSikdwikT[i][k]);                            // |4| b
                dEdwikdSmo[i][k].transpose();// now return to original                            |4| b
            }                                                                                  // |4| b
            for (int k=0; k<nN[i].val; k++) {                                                  // |4| a
                nimoXNoWtsIntoS[i].replace(dSikdwikdSimo[i][k]);                               // |4| a
                nimoXNoWtsIntoS[i].mult(dEdS[i].val(k));                                       // |4| a
                dEdwikdSmo[i][k].add(nimoXNoWtsIntoST[i]);                                     // |4| a
            }
            break;
        }// end switch
        if(i<numLayers) { // if i=numLayers there "u" is empty set
            for (int k=0; k<nN[i].val; k++) {                                                  // |2|
                dEdudwik[i][k].multColMat(dEdudS[i],k,dSikdwikT[i][k]);                        // |2|
                dEdudwik[i][k].transpose();                                                    // |2|
                dEdwikdu[i][k].replace(dEdudwik[i][k]);                                        // |2|
                dEdudwik[i][k].transpose();                                                    // |2|
            }                                                                                  // |2|
            dEdudSmo[i].mult(dEdudS[i],dSdSmoT[i]);                                            // |5|
        }
        dEdS[i-1].mult(dSdSmo[i],dEdS[i]);                                                     // (2)

        nimoXnimo[i].mult(0);
        switch(weightType[i-1].val) { // only make weights needed for this, the ith, layer
            default:
            case 0: // Linear
                nimoXone[i].mult(WT[i],dEdS[i]);                                               // (4) a
                nimoXone[i].multEl(App[i-1]);                                                  // (4) a
                nimoXnimo[i].diag(nimoXone[i]);                                                // (4) a
            break;
            case 1: // Quadratic1 with     full B (in*in) matrix and center (in*1)
            case 2: // Quadratic2 with (2) full B (in*in) matrix and center (in*1)
                for (int k=0; k<nN[i].val; k++) {                                              // (4) a
                    nimoXnimo[i].addMult(dEdS[i].val(k),dSikdSimodSimo[i][k]);                 // (4) a
                }                                                                              // (4) a
            break;
            case 3: // Quadratic with diagonal B (in*1) and center (in*1)
            break;
        }
        niXnimo[i].mult(dEdSdS[i],dSdSmoT[i]);                                                 // (4) b
        dEdSdS[i-1].mult(dSdSmo[i],niXnimo[i]);                                                // (4) b
        dEdSdS[i-1].add(nimoXnimo[i]);                                                         // (4) b
  } // end for
      } catch (MatrixException e) {
          e.print();
      }
      for (int i=numLayers; i>=0; i--) {
//          System.out.println("  dEdSdS["+i+"]  \n"+dEdSdS[i]);
          System.out.println("  dEdu["+i+"]  \n"+dEdu[i]);
//          System.out.println("  dEdudu["+i+"]  \n"+dEdudu[i]);
      }
      try {
         System.out.println("  dEdudu[0]  \n"+dEdudu[0].submatrix(0,0,1,dEdudu[0].nCols));
         // (0,0,1 .. is 0th weight, C10 in this case
      } catch (MatrixException e) {
          e.print();
      }
  }

  double expx,expmx;
  /** nonlinear activation function with x as the input
  */
  private final double activation(double x, int activationType) throws MatrixException
  {
     switch(activationType) {
        default:
        case 0: return(x);                          // I identity
        case 1: return(x>=0 ? x : 0);               // T step (hardlimiting squashing function)
        case 2: return((2/(1+Math.exp(-x)))-1);     // B bipolar sigmodal
        case 3: return(1/(1+Math.exp(-x)));         // M monopolar sigmoidal
        case 4: return(Math.exp(-x*x));             // G gaussian
//        case 5: return((Math.exp(x)-Math.exp(-x))/(Math.exp(x)+Math.exp(-x)));// H hyperbolic tangent
        case 5:
            expx  = Math.exp( x);
            expmx = Math.exp(-x);
            return((expx-expmx)/(expx+expmx));      // H hyperbolic tangent
        case 6: return(Math.sin(x));                // S Sin
        case 7: return(Math.exp(-x));               // E exponential inverse
    }
  } //end activation

  /** derivitive of nonlinear activation function where second param
    * is activation(input). Therefore activation() must
    * be called before this method
    */
  private final double activationPrime(double x, double f, int activationType) throws MatrixException
  {
    switch(activationType) {
        default:
        case 0: return(1);                          // I identity
        case 1: return(1);                          // T step (hardlimiting squashing function)
        case 2: return((1-(f*f))/2);                // B bipolar sigmodal
        case 3: return((1-f)*f);                    // M monopolar sigmoidal
        case 4: return(-2*f*x);                     // G gaussian
        case 5: return(1-f*f);                      // H hyperbolic tangent
        case 6: return(Math.cos(x));                // S Sin
        case 7: return(-f);                         // E exponential inverse
    }
  } //end activationPrime

  /** second derivitive of nonlinear activation function with x not as
    * input but rather activation(input).  Therefore
    * activation() must be called before this method
    */
  private final double activationDoublePrime(double x, double f, int activationType) throws MatrixException
  {
    switch(activationType) {
        default:
        case 0: return(0);                          // I identity
        case 1: return(0);                          // T step (hardlimiting squashing function)
        case 2: return(f*(f+1)*(f-1)/2);            // B bipolar sigmodal
        case 3: return((1-2*f)*f*(1-f));            // M monopolar sigmoidal
        case 4: return(2*f*(2*x*x - 1));            // G gaussian
        case 5: return(-2*(1-f*f)*f);               // H hyperbolic tangent
        case 6: return(-f);                         // S sin
        case 7: return(f);                          // E exponential inverse>
    }
  } //end activationDoublePrime

  /** Return # weights needed for nIn inputs (including the first
    * one which is always 1.0), and nOut outputs.
    */
  public int nWeights(int nIn,int nOut) {
    int numWeights=0;

    nN[0].val=nIn;
    nN[numLayers].val=nOut;

    for (int i=0; i<numLayers; i++) {
        switch(weightType[i].val) {
            default:
            case 0: // Linear
                numWeights+=nN[i].val*nN[i+1].val;
            break;
            case 1: // Quadratic1
            case 2: // Quadratic2
                numWeights+=nN[i].val*nN[i+1].val*(nN[i].val+1);
            break;
            case 3: // Diagonal
            break;
        }
    }
    return numWeights;
  }

  /** Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "'{' ('Identity' | 'HardlimitingSquashing' | 'Bipolar' | "+
                "'Monopolar' | 'ExponentialInverted' | 'HyperbolicTan' | "+
                "'Sin' | 'Gaussian') "+
                "('Linear' | 'Quadratic1'| 'Quadratic2') "+
                "(('Identity' | 'HardlimitingSquashing' | 'Bipolar' | "+
                "'Monopolar' | 'ExponentialInverted' | 'HyperbolicTan' | "+
                "'Sin' | 'Gaussian') '(' IntExp ')' "+
                "('Linear' | 'Quadratic1'| 'Quadratic2') ) *  "+
                "(('Identity' | 'HardlimitingSquashing' | 'Bipolar' | "+
                "'Monopolar' | 'ExponentialInverted' | 'HyperbolicTan' | "+
                "'Sin' | 'Gaussian') "+
   "//Neural network which computes the first and second "+
   "derivitives wrt the weights and inputs.  It's of the form "+
   "Net {a w a(#) w a(#) w a(#) w a} where a is an activation function, w is "+
   "a type of weights, and # is the number of nodes in a layer.  The layers "+
   "are listed in order from input to output.  A layer of 10 "+
   "sigmoids would be 'Linear Sigmoid(10)'.  A layer of 5 Radial Basis Functions (RBFs) "+
   "would be 'Quadratic? Gaussian(5)'. "+
   "Note that activation functions "+
   "must be in the first and last positions and these two activation functions should not "+
   "specifiy a number of nodes, because that information is given in the Data object. ";
    }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    u.emit("{");
    for (int i=0;i<=numLayers;i++) { // squashing function (activation function)
        switch(activationType[i].val) {
          case 0:  u.emit("Identity");              break;
          case 1:  u.emit("HardlimitingSquashing"); break;
          case 2:  u.emit("Bipolar");               break;
          case 3:  u.emit("Monopolar");             break;
          case 4:  u.emit("Gaussian");              break;
          case 5:  u.emit("HyperbolicTan");         break;
          case 6:  u.emit("Sin");                   break;
          case 7:  u.emit("ExponentialInverted");   break;
          default: u.emit("weightType");
                   u.emitUnparse(weightType[i],lang);break;
        }
      if (i>0 && i<numLayers) { //a hidden layer, so give # nodes
        u.emit("( ");
        u.emitUnparse(nN[i],lang);
        u.emit(")");
      }
      if (i<numLayers) //type of weights
        switch(weightType[i].val) {
            case 0:  u.emit(" Linear ");     break;
            case 1:  u.emit(" Quadratic1 "); break;
            case 2:  u.emit(" Quadratic2 "); break;
            default: u.emit(" activationType");
                     u.emitUnparse(activationType[i],lang);break;
        }
    }
    u.emit("}");
    u.emitLine();
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    Vector v =new Vector();
    Vector v1=new Vector();
    Vector v2=new Vector();
    IntExp num;

    p.parseChar('{',true);

    if      (p.parseID("HardlimitingSquashing",false)) {v1.addElement(new IntExp(1));}
    else if (p.parseID("Bipolar"              ,false)) {v1.addElement(new IntExp(2));}
    else if (p.parseID("Monopolar"            ,false)) {v1.addElement(new IntExp(3));}
    else if (p.parseID("Gaussian"             ,false)) {v1.addElement(new IntExp(4));}
    else if (p.parseID("HyperbolicTan"        ,false)) {v1.addElement(new IntExp(5));}
    else if (p.parseID("Sin"                  ,false)) {v1.addElement(new IntExp(6));}
    else if (p.parseID("ExponentialInverted"  ,false)) {v1.addElement(new IntExp(7));}
    else    {p.parseID("Identity"             ,true );  v1.addElement(new IntExp(0));}

    while (true) {
        if        (p.parseID("Quadratic1" ,false)) {v2.addElement(new IntExp(1));}
        else if   (p.parseID("Quadratic2" ,false)) {v2.addElement(new IntExp(2));}
        else      {p.parseID("Linear"     ,true );  v2.addElement(new IntExp(0));}

        if      (p.parseID("HardlimitingSquashing",false)) {v1.addElement(new IntExp(1));}
        else if (p.parseID("Bipolar"              ,false)) {v1.addElement(new IntExp(2));}
        else if (p.parseID("Monopolar"            ,false)) {v1.addElement(new IntExp(3));}
        else if (p.parseID("Gaussian"             ,false)) {v1.addElement(new IntExp(4));}
        else if (p.parseID("HyperbolicTan"        ,false)) {v1.addElement(new IntExp(5));}
        else if (p.parseID("Sin"                  ,false)) {v1.addElement(new IntExp(6));}
        else if (p.parseID("ExponentialInverted"  ,false)) {v1.addElement(new IntExp(7));}
        else    {p.parseID("Identity"             ,true );  v1.addElement(new IntExp(0));}

        if (p.parseChar('(',false)) {
            v.addElement((IntExp)p.parseClass("IntExp",lang,false));
            p.parseChar(')',true);
        } else
          break;
    }
    p.parseChar('}',true);
    numLayers=v.size()+1;
    nN              =new IntExp[numLayers+1];
    activationType  =new IntExp[numLayers+1];
    weightType      =new IntExp[numLayers+2];
    noWtsIntoS      =new IntExp[numLayers+1];

    v.copyInto(  nN);
    v1.copyInto( activationType);
    v2.copyInto( weightType);

    for (int i=nN.length-1;i>0;i--)
        nN[i]=nN[i-1];
    nN[0]              =new IntExp(0);  //# will be filled in by setIO()
    nN[numLayers]      =new IntExp(0);  //# will be filled in by setIO()
    return this;
  } //end parse

  /** Make an exact duplicate of this class.  For objects it contains, it
    * only duplicates the pointers, not the objects they point to.  For a
    * new FunApp called MyFunApp, the code in this method should be the
    * single line: return cloneVars(new MyFunApp());
    */
  public Object clone() {
    return cloneVars(new Net());
  }

  /** After making a copy of self during a clone(), call cloneVars() to
    * copy variables into the copy, then return super.cloneVars(copy).
    * The variables copied are just those set in parse() and
    * setWatchManager().  The caller will be required to call
    * setIO to set up the rest of the variables.
    */
  public Object cloneVars(FunApp copy) {
    Net c=(Net)copy;
    c.nN            =nN;
    c.weightType    =weightType;
    c.activationType=activationType;
    c.noWtsIntoS    =noWtsIntoS;
    c.numLayers     =numLayers;
    return super.cloneVars(copy);
  }
} //end class Net

/*  To check this code make sure the network architecture is as given in the .html file :

`embed
`(0,0,350,130)
`Simulator {
`   experiment Backprop {
`     rate     0.1
`     momentum  0.0
`     tolerance 0   //this was .01, but infinite loops are better demos
`     smooth    .9
`     initWeights  -.1,.1  //random initial weights in this range
`     error
`       batch {
`         batchSize 1
`         SupervisedLearning {
`           incremental true //not epoch-wise training
`           data        table {  //each row is an input vector, output vector
`                         [1] [0]
`                       }
`           funApp   Net { Identity
`                             Quadratic1
`                          ExponentialInverted(2)
`                             Quadratic1
`                          ExponentialInverted(2)
`                             Linear
`                          Identity }
`         }
`       }
`     }
`   displays {
`  }
`}

and then SupervisedLearning (which contains the error function) should fill dEdOutdOut
and dEdOut as follows to prepare it for the same cost function used in mathematica.  Right
before the line

        data.getData(inputs.data,dEdOut.data,rnd); //get desired outputs

add the code

        data.getData(0,inputs.data,dEdOut.data); //get desired outputs
        function.evaluate();                               //calculate actual outputs
        dEdOut.subFrom(outputs);                   //actual-desired=gradient of error wrt outputs
        dEdOutdOut.mult(0);
        dEdOutdOut.diag(1);
        function.findHessian();
        System.out.println("  dEdIn  \n"+dEdIn);
        System.out.println("  dEdIndIn  \n"+dEdIndIn);
        System.out.println("---------------------  LL finished LL ---------------------");
        System.exit(0);

which is a result of making the cost function, .5 * error^2. And of course you should
have the appropriate System.out.prinln statements uncommented in Net.java (this file) as:

      for (int i=numLayers; i>=0; i--) {
          System.out.println("  dEdu["+i+"]  \n"+dEdu[i]);
      }
      try {
         System.out.println("  dEdudu[0]  \n"+dEdudu[0].submatrix(0,0,1,dEdudu[0].nCols));
         // (0,0,1 .. is 0th weight, C10 in this case
      } catch (MatrixException e) {
          e.print();
      }

//which can be found at the bottom of findHessian().
This is a SISO two layer with 2 quadratic layers, 2 nodes in each.  Then the output
should match this as follows:

  dEdu[0]
[-0.00014395573213, -0.0009233737471, 5.17317174171606E-6, 0.00141624068065, -0.0005701512998, 0.00110415861607, -0.009581288780
54, -0.0081290072337, -0.0081290072337, -0.0068968549137, -0.00012704130184, 0.00015255772661, 0.00103162941756, 0.0009582508528
8, 0.00095825085288, 0.00089009161761, -0.08029973205767, -0.08332585610966] transpose
  dEdudu[0]
[0.00019659585141, 0.0021053402734, -3.38493437328989E-7, -0.00009266813475, 0.00016424967436, -0.00008562772135, 0.003880230890
42, 0.00179477900302, 0.00179477900302, 0.00025238290688, 0.0000203819403, 3.39765906070399E-6, -0.00036848961196, -0.0001659377
9143, -0.00016593779143, 9.66379426816948E-6, 0.0029384813879, -0.0009046753331]
  dEdIn
[0.00013878256039]
  dEdIndIn
[0.0001910074164]
---------------------  LL finished LL ---------------------

which is in concert with mathematica's

(* This test a 2 node 3 layer SISO RBF 10 June 97*)
Clear[S11,S12,C10,B10,C11,B11,C201,C202,B2011,B2012,B2021,B2022,C211,C212,
  B2111,B2112,B2121,B2122,W1,W2,x];
B20={{B2011,B2012},{B2021,B2022}};
B21={{B2111,B2112},{B2121,B2122}};
C20={C201,C202};
C21={C211,C212};
W={W1,W2};
Act0[x_]:=x;
Act1[x_]:=Exp[-x];
Act2[x_]:=Exp[-x];
Act3[x_]:=x;
A0=Act0[x];
S11=(A0-C10) B10 (A0-C10);
A11=Act1[S11];
S12=(A0-C11)   B11(A0-C11);
A12=Act1[S12];
A1={A11,A12};
S21=(A1-C20) .B20.(A1-C20);
A21=Act2[S21];
S22=(A1-C21) .B21.(A1-C21);
A22=Act2[S22];
A2={A21,A22};
output=Act3[A2 . W];
Err=(1/2)output^2;
Dww={
		D[D[Err,C10],C10],
		D[D[Err,C10],B10],
		D[D[Err,C10],C11],
		D[D[Err,C10],B11],
		D[D[Err,C10],C201],
		D[D[Err,C10],C202],
		D[D[Err,C10],B2011],
		D[D[Err,C10],B2012],
		D[D[Err,C10],B2021],
		D[D[Err,C10],B2022],
		D[D[Err,C10],C211],
		D[D[Err,C10],C212],
		D[D[Err,C10],B2111],
		D[D[Err,C10],B2112],
		D[D[Err,C10],B2121],
		D[D[Err,C10],B2122],
		D[D[Err,C10],W1],
		D[D[Err,C10],W2]};
Dw={
		D[Err,C10],
		D[Err,B10],
		D[Err,C11],
		D[Err,B11],
		D[Err,C201],
		D[Err,C202],
		D[Err,B2011],
		D[Err,B2012],
		D[Err,B2021],
		D[Err,B2022],
		D[Err,C211],
		D[Err,C212],
		D[Err,B2111],
		D[Err,B2112],
		D[Err,B2121],
		D[Err,B2122],
		D[Err,W1],
		D[Err,W2]};
dEdxdx=D[D[Err,x],x];
dEdx=D[Err,x];
C10   = -0.09363323840477;
B10   = -0.0852497561362;
C11   = -0.06041776503166;
B11   = -0.00193671996977;

B2011  = -0.04553876423664;
B2012  =  0.00601807600102;
B2021  =  0.02270677079075;
B2022  =  0.05920360237961;

C201   = -0.01365207259737;
C202   =  0.05110143466733;

B2111  =   0.06430651386567;
B2112  =   0.03223987989879;
B2121  =  -0.02194830288469;
B2122  =  -0.09485508418878;

C211    = -0.01467918450901;
C212    = -0.04003197539648;

W1     = -0.09495198206434;
W2     =  0.00983428188337;
x=1;
N[Dw,10]
N[Dww,10]
N[dEdx,10]
N[dEdxdx,10]

**/
