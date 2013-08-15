package com.hbaspecto.pecas.sd.estimation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;

import com.hbaspecto.discreteChoiceModelling.Coefficient;

public class EstimationMatrix{
    private final Vector expvalues;
	// Rows of targets, columns of coefficients, linearized relationship between targets and coefficients
    private final Matrix derivatives;
    
	private final List<ExpectedValue> expValues;
	
	private final List<Coefficient> coefficients;
	
	public EstimationMatrix(List<ExpectedValue> eVals, List<Coefficient> coefficients) {
	    this.expValues = new BackMapList<ExpectedValue>(eVals);
	    this.coefficients = new BackMapList<Coefficient>(coefficients);
	    derivatives = new DenseMatrix(eVals.size(), coefficients.size());
	    expvalues = new DenseVector(eVals.size());
	}
	
	public void addExpectedValueComponent(Vector component) {
	    expvalues.add(component);
	}
	
	public void addExpectedValueComponentApplicableToCurrentParcel(Vector component) {
	    int i = 0;
	    int j = 0;
	    for(ExpectedValue t : expValues) {
	        if(t.appliesToCurrentParcel()) {
	            expvalues.add(i, component.get(j));
	            j++;
	        }
	        i++;
	    }
	}
	
	/**
	 * Adds the specified values, element by element, to the cumulative totals for the derivatives.
	 * @param component
	 * @throws IndexOutOfBoundsException if the given matrix is the wrong size.
	 */
	public void addDerivativeComponent(Matrix component) {
	    derivatives.add(component);
	}
	
   public void addDerivativeComponentApplicableToCurrentParcel(Matrix component) {
        int i = 0;
        int j = 0;
        for(ExpectedValue t : expValues) {
            if(t.appliesToCurrentParcel()) {
                for(int k = 0; k < derivatives.numColumns(); k++)
                    derivatives.add(i, k, component.get(j, k));
                j++;
            }
            i++;
        }
    }

	/**
	 * Returns an immutable list holding the targets. The list has a back-map from target
	 * objects to indices, so calls to <code>indexOf</code> and <code>contains</code>
	 * have constant time performance.
	 * @return An immutable list of targets.
	 */
	public List<ExpectedValue> getTargets() {
		return expValues;
	}
	
	/**
	 * Returns an immutable list (similar to <code>getTargets</code>) containing only
	 * those targets that proclaim themselves to be valid on the current parcel.
	 * @return An immutable list of the targets applicable to the current parcel.
	 */
	public List<ExpectedValue> getTargetsApplicableToCurrentParcel() {
	    List<ExpectedValue> sublist = new ArrayList<ExpectedValue>();
	    for(ExpectedValue t : expValues)
	        if(t.appliesToCurrentParcel())
	            sublist.add(t);
	    
	    return new BackMapList<ExpectedValue>(sublist);
	}

	/**
	 * Returns an immutable list holding the coefficients. The list has a back-map from
	 * coefficient objects to indices, so calls to <code>indexOf</code> and
	 * <code>contains</code> have constant time performance.
     * @return An immutable list of targets.
	 */
	public List<Coefficient> getCoefficients() {
		return coefficients;
	}
	
	public Vector getExpectedValues() {
	    return expvalues.copy();
	}
	
	public Matrix getDerivatives() {
	    return derivatives.copy();
	}
	
	// A wrapper around another list that prevents it from being
	// modified and provides fast lookup via a back-map.
	private class BackMapList<E> implements List<E> {
	    private List<E> contents;
	    // The integer array contains two indices - the first is the first index
	    // of that element (for indexOf()), the second is the last index of that
	    // element (for lastIndexOf()).
	    private HashMap<E, int[]> backmap;
	    
	    private BackMapList(List<E> list) {
            contents = list;
	        backmap = new HashMap<E, int[]>();
	        Iterator<E> it = list.iterator();
	        int i = 0;
	        while(it.hasNext()) {
	            E next = it.next();
	            if(backmap.containsKey(next)) {
	                int[] indices = backmap.get(next);
	                // The last occurrence of the element is now at i.
	                indices[1] = i;
	            }
	            else {
	                int[] indices = new int[2];
	                indices[0] = i;
	                indices[1] = i;
	                backmap.put(next, indices);
	            }
	            i++;
	        }
	    }
	    
	    @Override
        public boolean add(E e) { throw new UnsupportedOperationException();}
	    
	    @Override
        public void add(int index, E element) { throw new UnsupportedOperationException();}
	    
	    @Override
        public boolean addAll(Collection<? extends E> c) { throw new UnsupportedOperationException();}
	    
	    @Override
        public boolean addAll(int index, Collection<? extends E> c) {throw new UnsupportedOperationException();}
	    
	    @Override
        public void clear() { throw new UnsupportedOperationException();}
	    
	    @Override
        public boolean contains(Object o) { return backmap.containsKey(o);}
	    
	    @Override
        public boolean containsAll(Collection<?> c) { return backmap.keySet().containsAll(c);}
	    
	    @Override
        @SuppressWarnings("rawtypes")
        public boolean equals(Object o) {
	        if(o == this)
	            return true;
	        if(!(o instanceof BackMapList))
	            return false;
	        BackMapList other = (BackMapList) o;
	        return contents.equals(other.contents);
	    }
	    
	    @Override
        public E get(int index) { return contents.get(index);}
	    
	    @Override
        public int hashCode() { return contents.hashCode() + 1;}
	    
	    @Override
        public int indexOf(Object o) {
	        if(this.contains(o))
	            return backmap.get(o)[0];
	        else
	            return -1;
	    }
	    
	    @Override
        public boolean isEmpty() { return contents.isEmpty();}
	    
	    @Override
        public Iterator<E> iterator() { return Collections.unmodifiableList(contents).iterator();}
	    
	    @Override
        public int lastIndexOf(Object o) {
	        if(this.contains(o))
                return backmap.get(o)[1];
            else
                return -1;
	    }
	    
	    @Override
        public ListIterator<E> listIterator() {
	        return Collections.unmodifiableList(contents).listIterator();
	    }
	    
	    @Override
        public ListIterator<E> listIterator(int index) {
	        return Collections.unmodifiableList(contents).listIterator(index);
	    }
	    
	    @Override
        public E remove(int index) { throw new UnsupportedOperationException();}
	    
	    @Override
        public boolean remove(Object o) { throw new UnsupportedOperationException();}
	    
	    @Override
        public boolean removeAll(Collection<?> c) { throw new UnsupportedOperationException();}
	    
	    @Override
        public boolean retainAll(Collection<?> c) { throw new UnsupportedOperationException();}
	    
	    @Override
        public E set(int index, E element) { throw new UnsupportedOperationException();}
	    
	    @Override
        public int size() { return contents.size();}
	    
	    @Override
        public List<E> subList(int fromIndex, int toIndex) {
	        return new BackMapList<E>(contents.subList(fromIndex, toIndex));
	    }
	    
	    @Override
        public Object[] toArray() { return contents.toArray();}
	    
	    @Override
        public <T> T[] toArray(T[] a) { return contents.toArray(a);}
	}
}



