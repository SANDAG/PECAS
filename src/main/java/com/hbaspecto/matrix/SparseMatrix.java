package com.hbaspecto.matrix;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import no.uib.cipr.matrix.AbstractMatrix;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.MatrixEntry;

public class SparseMatrix extends AbstractMatrix {
	
	HashMap<Index,Double> values = new HashMap<Index,Double>();
	
	class Index implements MatrixEntry {
		final int row;
		final int col;
		
		Index(int row, int col) {
			this.row = row;
			this.col = col;
		}

		@Override
		public boolean equals(Object obj) {
			if (! (obj instanceof Index)) return false;
			Index i = (Index) obj;
			return ((row == i.row) && (col == i.col));
		}

		@Override
		public int hashCode() {
			return row*numColumns+col;
		}

		@Override
		public String toString() {
			return "idx "+row+","+col;
		}

		public int column() {
			return col;
		}

		public double get() {
			Double value = values.get(this);
			if (value==null) return 0;
			return value.doubleValue();
		}

		public int row() {
			return row;
		}

		public void set(double value) {
			if (value ==0) {
				values.remove(this);
			} else {
				values.put(this, value);
			}
		}
	}

	public SparseMatrix(int numRows, int numColumns) {
		super(numRows, numColumns);
	}
	
    public Iterator<MatrixEntry> iterator() {
        return new SparseMatrixIterator();
    }
    /**
     * Iterator over a general matrix. Uses column-major traversal
     */
    class SparseMatrixIterator implements Iterator<MatrixEntry> {
    	
    	Iterator valuesIterator;
    	
    	SparseMatrixIterator() {
    		valuesIterator = values.entrySet().iterator();
    	}
    	
        public boolean hasNext() {
            return valuesIterator.hasNext();
        }

        public MatrixEntry next() {
        	return (MatrixEntry) ((Entry) valuesIterator.next()).getKey();
        }

        public void remove() {
            valuesIterator.remove();
        }

    }


	@Override
	public Matrix copy() {
		SparseMatrix newOne = new SparseMatrix(numRows, numColumns);
		newOne.values = (HashMap<Index, Double>) values.clone();
		return newOne;
	}

	@Override
	public double get(int row, int column) {
		Index idx = new Index(row,column);
		Double value = values.get(idx);
		if (value == null) return 0;
		return value.doubleValue();
	}

	@Override
	public void set(int row, int column, double value) {
		Index idx = new Index(row,column);
		if (value ==0) {
			values.remove(idx);
		} else {
			values.put(idx, new Double(value));
		}
	}
}
