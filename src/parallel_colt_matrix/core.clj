(ns parallel-colt-matrix.core
  (:use [core.matrix.protocols])
  (:require [core.matrix.implementations :as imp])
  (:import [cern.colt.matrix.tdouble.impl DenseDoubleMatrix2D])
  (:import [cern.jet.math.tdouble DoubleFunctions])
  (:import [cern.colt.matrix.tdouble.algo DenseDoubleAlgebra]))


(extend-type DenseDoubleMatrix2D
  PImplementation
  (implementation-key [m]
    "Returns a keyword representing this implementation. 
     Each implementation should have one unique key."
    :parallel-colt)
  (construct-matrix [m data]
    "Returns a new matrix containing the given data. Data should be in the form of either nested sequences or a valid existing matrix"
    (let [data (if-not (vector? data)
                 (convert-to-nested-vectors data)
                 data)]
     (DenseDoubleMatrix2D. (into-array (map #(into-array Double/TYPE (map double %)) data)))))
  (new-vector [m length]
    "Returns a new vector (1D column matrix) of the given length."
    (throw (Exception. "Only 2D matrix, use new-matrix")))
  (new-matrix [m rows columns]
    "Returns a new matrix (regular 2D matrix) with the given number of rows and columns."
    (DenseDoubleMatrix2D. rows columns))
  (new-matrix-nd [m shape]
    "Returns a new general matrix of the given shape.
     Shape must be a sequence of dimension sizes."
    (throw (Exception. "Only 2D matrix, use new-matrix")))

   (supports-dimensionality? [m dimensions]
     "Returns true if the implementation supports matrices with the given number of dimensions."
     (== 2 dimensions))

  PDimensionInfo
  (dimensionality [m] 
    "Returns the number of dimensions of a matrix"
    2)
  (get-shape [m]
    "Returns the shape of the matrix, as an array or sequence of dimension sizes"
    [(.rows m) (.columns m)])
  (is-scalar? [m] 
    "Tests whether an object is a scalar value"
    false)
  (is-vector? [m] 
    "Tests whether an object is a vector (1D matrix)"
    false)
  (dimension-count [m dimension-number]
    "Returns the size of a specific dimension
I assumed 0 for colunms 1 for rows"
    (assert (< dimension-number 3))
    (assert (>= dimension-number 0))
    (case dimension-number
      0 (.rows m)
      1 (.columns m)))

  PIndexedAccess
  (get-1d [m row]
    (-> (.viewRow m row) (.toArray) (vec)))
  (get-2d [m row column]
    (.get m row column))
  (get-nd [m indexes]
    (throw (Exception. "It is only a 2D Array")))

  PIndexedSetting
  (set-1d [m row v]
    (throw (Exception. "It is a 2D matrix, specify another dimension, use set-2d(!)")))
  (set-2d [m row column v]
    (let [other (.copy m)]  ;;Keeping immutability
      (.set other row column v)
      other)) ;;We could use .setQuick but then we need
  ;;to add a pre-condition to check if the index is in the bound, and
  ;;I guess it will be slower than simply use .set
  (set-nd [m indexes v]
    (throw (Exception. "It is a 2D matrix, no other dimension, use set-2d(!)")))
  (is-mutable? [m]
    true)
  
  ;; PSpecialisedConstructors
  ;; (identity-matrix [m dims] "Create a 2D identity matrix with the given number of dimensions"
  ;;   (DenseDoubleMatrix2D. dims dims)) ;;Ready just wait for
  ;; protocols to be updated in clojars
  ;; (diagonal-matrix [m diagonal-values] "Create a diagonal matrix with the specified leading diagonal values") ;;TODO USE SPARSE ?

  PCoercion
  (coerce-param [m param]
    "Attempts to coerce param into a matrix format supported by the implementation of matrix m.
     May return nil if unable to do so, in which case a default implementation can be used."
    nil)
  
  PMatrixEquality
  (matrix-equals [a b]
    (.equals a b))
  
  ;; PAssignment ;;TODO
  ;; "Protocol for assigning values to mutable matrices."
  ;; (assign-array! [m arr] "Sets all the values in a matrix from a Java array, in row-major order")
  ;; (assign! [m source] "Sets all the values in a matrix from a matrix source")

  PMatrixMultiply
  (matrix-multiply [m a]
    (.zMult m a nil))
  (element-multiply [m a]
    (let [multiplier (. DoubleFunctions mult a)
          other (.copy m)]
      (.assign other multiplier)))

  ;; PVectorTransform ;;TODO
  ;; "Protocol to support transformation of a vector to another vector. 
  ;;  Is equivalent to matrix multiplication when 2D matrices are used as transformations.
  ;;  But other transformations are possible, e.g. affine transformations."
  ;; (vector-transform [m v] "Transforms a vector")
  ;; (vector-transform! [m v] "Transforms a vector in place - mutates the vector argument")

  ;; PMatrixScaling ;;TODO
  ;; "Protocol to support matrix scaling by scalar values"
  ;; (scale [m a])
  ;; (pre-scale [m a])
  
  PMatrixAdd
  (matrix-add [m a]
    (assert (= (get-shape m) (get-shape a)))
    (let [sum (. DoubleFunctions plus)
          other (.copy m)]
      (.assign other a sum)))
  (matrix-sub [m a]
    (assert (= (get-shape m) (get-shape a)))
    (let [minus (. DoubleFunctions minus)
          other (.copy m)]
      (.assign other a minus)))

  ;; PVectorOps ;;TODO
  ;; "Protocol to support common vector operations."
  ;; (vector-dot [a b])
  ;; (length [a])
  ;; (length-squared [a])
  ;; (normalise [a])

  PMatrixOps
  (trace [m]
    (.trace (DenseDoubleAlgebra.) m))
  (determinant [m]
    (.det (DenseDoubleAlgebra.) m))
  (inverse [m]
    (.inverse (DenseDoubleAlgebra.) m))
  (negate [m]
    (element-multiply m -1))
  (transpose [m]
    (.transpose (DenseDoubleAlgebra.) m))

  ;; PMathsFunctions ;;TODO

  PMatrixSlices ;;TODO
  (get-row [m i]
    (-> (.viewRow m i) (.toArray) (vec))) ;; same as get-1d
  (get-column [m i]
    (-> (.viewColumn m i) (.toArray) (vec)))
  (get-major-slice [m i]
    (get-row m i)) ;; ???? DON'T GET IT
  (get-slice [m dimension i]
    (condp == dimension
      0 (get-row m i)
      1 (get-column m i)
      (throw (Exception. "It is a 2D matrix, it only have 2 dimension"))))
  
  ;; PFunctionalOperations  ;;TODO
  ;; "Protocol to allow functional-style operations on matrix elements."
  ;; ;; note that protocols don't like variadic args, so we convert to regular args
  ;; (element-seq [m])
  ;; (element-map [m f]
  ;;              [m f a]
  ;;              [m f a more])
  ;; (element-map! [m f]
  ;;               [m f a]
  ;;               [m f a more])
  ;; (element-reduce [m f] [m f init])

  PConversion
  (convert-to-nested-vectors [m]
    (->> (.toArray m) (map vec) vec)))

(imp/register-implementation (DenseDoubleMatrix2D. 2 2))
