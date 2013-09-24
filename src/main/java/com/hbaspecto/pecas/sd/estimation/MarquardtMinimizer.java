package com.hbaspecto.pecas.sd.estimation;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import no.uib.cipr.matrix.BandMatrix;
import no.uib.cipr.matrix.DenseCholesky;
import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;
import org.apache.log4j.Logger;

public class MarquardtMinimizer
{
    private static Logger          logger         = Logger.getLogger(MarquardtMinimizer.class);
    private static final int       CONVERGED      = 0;
    private static final int       MAX_ITERATIONS = 1;
    private static final int       INVALID_STEP   = 2;
    private static final int       NOT_RUN        = -1;

    private static final double    MINIMUM_LAMBDA = 1E-7;
    private static final double    MAXIMUM_LAMBDA = 1E6;

    private Collection<Constraint> constraints;
    private ObjectiveFunction      obj;
    private double                 minstep;
    private double                 maxstep;
    private double                 lambda;
    private double                 initialLambda;

    private Vector                 parameters;

    private double                 currentObjective;

    private int                    iteration;
    private int                    termination;
    // private int damper;

    private int                    penaltyIteration;
    private double                 alpha;
    private boolean                penaltyConverged;

    /**
     * Constructs a new optimizer that minimizes the given objective function.
     * By default, the minimum step size is 0.0001, the maximum step size is 1,
     * and the initial Marquardt weighting factor is 0.01.
     * 
     * @param obj
     *            The objective function.
     * @param initialGuess
     *            Values of the objective function's parameters to use as an
     *            initial guess.
     * @throws OptimizationException
     *             if the initial guess causes an error in the objective
     *             function.
     */
    public MarquardtMinimizer(ObjectiveFunction objective, Vector initialGuess)
            throws OptimizationException
    {
        constraints = new ArrayList<Constraint>();
        obj = objective;
        minstep = 0.0001;
        maxstep = 1;
        lambda = 600;
        initialLambda = lambda;

        parameters = initialGuess.copy();

        obj.setParameterValues(parameters);

        try
        {
            currentObjective = obj.getValue() + getPenaltyFunction(parameters);
        } catch (OptimizationException e)
        {
            String msg = "Objective function could not be evaluated at guess";
            throw new OptimizationException(msg, e);
        }

        iteration = 0;
        penaltyIteration = 0;
        alpha = 0.001 * currentObjective;
        // damper = 1;
        termination = NOT_RUN;
    }

    /**
     * Readies the optimizer for a new optimization, using the given initial
     * guess.
     * 
     * @param initialGuess
     *            The new initial guess.
     * @throws OptimizationException
     *             if the initial guess causes an error in the objective
     *             function.
     */
    public void reset(Vector initialGuess) throws OptimizationException
    {
        parameters = initialGuess.copy();
        obj.setParameterValues(parameters);
        try
        {
            currentObjective = obj.getValue() + getPenaltyFunction(parameters);
        } catch (OptimizationException e)
        {
            String msg = "Objective function could not be evaluated at guess";
            throw new OptimizationException(msg, e);
        }
        lambda = initialLambda;
        iteration = 0;
        // damper = 1;
    }

    /**
     * Readies the optimizer for a new constrained optimization, using the given
     * initial guess. This method is similar to <code>reset</code> except that
     * it also resets the constraint looseness and number of penalty function
     * iterations.
     * 
     * @param initialGuess
     *            The new initial guess.
     * @throws OptimizationException
     *             if the initial guess causes an error in the objective
     *             function.
     */
    public void resetPenalty(Vector initialGuess) throws OptimizationException
    {
        reset(initialGuess);
        penaltyIteration = 0;
        alpha = 0.001 * currentObjective;
    }

    /**
     * Sets the parameters for optimization.
     * 
     * @param minStepSize
     *            The minimum step size allowed for an iteration.
     * @param maxStepSize
     *            The maximum step size allowed for an iteration.
     * @param initialMarquardtFactor
     *            The factor at which the diagonal matrix B in the Marquardt
     *            method is weighted compared to the Hessian.
     */
    public void setOptimizationParameters(double minStepSize, double maxStepSize,
            double initialMarquardtFactor)
    {
        minstep = minStepSize;
        maxstep = maxStepSize;
        lambda = initialMarquardtFactor;
        initialLambda = lambda;
    }

    /**
     * Sets the initial Newton/gradient descent factor (large values make the
     * step size smaller and more like gradient descent).
     * 
     * @param initialMarquardtFactor
     *            The initial factor.
     */
    public void setInitialMarquardtFactor(double initialMarquardtFactor)
    {
        lambda = initialMarquardtFactor;
        initialLambda = lambda;
    }

    /**
     * Adds a constraint to the minimizer, confining the solution into a
     * feasible region.
     * 
     * @param cons
     *            The constraint.
     */
    public void addConstraint(Constraint cons)
    {
        constraints.add(cons);
    }

    /**
     * Returns all the constraints on this minimizer.
     * 
     * @return The constraints.
     */
    public Collection<Constraint> getConstraints()
    {
        return new ArrayList<Constraint>(constraints);
    }

    /**
     * Removes all constraints on this minimizer.
     */
    public void clearConstraints()
    {
        constraints.clear();
    }

    /**
     * Performs a single iteration of the Marquardt method.
     * 
     * @return The new values of the objective function's parameters after the
     *         iteration.
     */
    public Vector doOneIteration() throws OptimizationException
    {
        Vector gradient = obj.getGradient(parameters).add(getPenaltyFunctionGradient(parameters));
        Matrix origHessian = obj.getHessian(parameters).add(getPenaltyFunctionHessian(parameters));

        // Write out derivative matrix
        BufferedWriter writerD = null;
        BufferedWriter writerH = null;
        try
        {
            writerD = new BufferedWriter(new FileWriter("derivs" + iteration + ".csv"));
            ((GaussBayesianObjective) obj).printCurrentDerivatives(writerD);
            writerH = new BufferedWriter(new FileWriter("hessian" + iteration + ".csv"));
            ((GaussBayesianObjective) obj).printHessian(writerH, origHessian);
        } catch (IOException e)
        {
        } finally
        {
            if (writerD != null) try
            {
                writerD.close();
            } catch (IOException e)
            {
            }
            if (writerH != null) try
            {
                writerH.close();
            } catch (IOException e)
            {
            }
        }

        // TODO if hessian has NaN's the PREVIOUS step was bad. Should check for
        // those.
        Matrix correction = getMarquardtCorrection(origHessian);

        boolean foundValidStep = false;

        Vector newparams = null;
        while (!foundValidStep)
        {
            // add lambda*correction to hessian, to adjust step size (large
            // lambda means small step size and more like steepest descent)
            Matrix hessian = origHessian.copy();
            hessian.add(lambda, correction);

            // Solve the system of equations using Cholesky decomposition.
            DenseCholesky cholesky = DenseCholesky.factorize(hessian);
            Matrix mstep = cholesky.solve(new DenseMatrix(gradient));
            Vector step = convertColumnMatrixToVector(mstep);
            step = step.scale(-1);

            // Find the new parameter values.
            newparams = new DenseVector(parameters);
            newparams.add(step);
            obj.setParameterValues(newparams);
            obj.logParameters(logger);
            try
            {
                double newobj = obj.getValue() + getPenaltyFunction(newparams);
                obj.logTargetAndObjective(logger);
                // Determine if the step is acceptable, readjust parameters for
                // next iteration.
                if (newobj < currentObjective)
                {
                    // Step may be is acceptable.
                    logger.info("Potentially good step, checking...");
                    Vector oldParameters = parameters; // just in case
                    parameters = newparams;
                    Matrix newHessian = obj.getHessian(parameters).add(
                            getPenaltyFunctionHessian(parameters));
                    Double hessianNorm = new Double(newHessian.norm(Matrix.Norm.One));
                    if (hessianNorm.isInfinite() || hessianNorm.isNaN())
                    {
                        // ooops, this is a bad step.
                        parameters = oldParameters;
                        lambda = 2 * lambda;
                        logger.info("***Step looked good but hessian had infinitiy or NaN in it so we have to back up, lambda set to "
                                + lambda);
                        foundValidStep = false;
                    } else
                    {
                        currentObjective = newobj;
                        lambda = Math.max(0.9 * lambda, MINIMUM_LAMBDA);
                        logger.info("Found valid step, setting lambda to " + lambda);
                        // damper = damper / 2;
                        foundValidStep = true;
                    }
                } else
                {
                    logger.info("Step leads to worse goodness of fit, backing up");
                    // foundValidStep = interpolateStepSize(step, gradient,
                    // hessian, newobj);
                    lambda = 2 * lambda;
                    logger.info("Interpolated step, now setting lambda to " + lambda);
                }
            } catch (OptimizationException e)
            {
                logger.info("Overflow error, backing up");
                foundValidStep = false;
                lambda = 2 * lambda;
            }

            if (lambda > MAXIMUM_LAMBDA)
            {
                throw new OptimizationException("Cannot find a valid step");
            }
        }

        logger.info("Finished iteration " + iteration + ".");

        iteration++;

        return parameters.copy();
    }

    private double getPenaltyFunction(Vector params)
    {
        double penalty = 0.0;
        for (Constraint cons : constraints)
            penalty += cons.getPenaltyFunction(params, alpha);
        return penalty;
    }

    private Vector getPenaltyFunctionGradient(Vector params)
    {
        Vector gradient = new DenseVector(parameters.size());
        for (Constraint cons : constraints)
            gradient.add(cons.getPenaltyFunctionGradient(params, alpha));
        return gradient;
    }

    private Matrix getPenaltyFunctionHessian(Vector params)
    {
        Matrix hessian = new DenseMatrix(parameters.size(), parameters.size());
        for (Constraint cons : constraints)
            hessian.add(cons.getPenaltyFunctionHessian(params, alpha));
        return hessian;
    }

    // Returns true if a valid step size was found.
    // private boolean interpolateStepSize(Vector step, Vector gradient, Matrix
    // hessian, double newobj) {
    // Vector currentStep = new DenseVector(step.size());
    // Vector currentParams = new DenseVector(parameters.size());
    // double currentObj = newobj;
    //
    // // Initialize.
    // double a = constraints.size() == 0? 1 : 0.5;
    // double stepsize = Math.pow(2, -damper) * Math.min(a, maxstep);
    // double gamma = -quadraticForm(gradient, hessian);
    //
    // while(currentObj >= currentObjective) {
    // double stepstar = gamma * stepsize * stepsize / (2 * (gamma * stepsize +
    // currentObjective - currentObj));
    // stepsize = Math.max(0.25 * stepsize, Math.min(0.75 * stepsize,
    // stepstar));
    // if(stepsize <= minstep)
    // // We can't find a valid step size (i.e. in that direction, everything is
    // uphill). Keep the old parameter values.
    // return false;
    //
    // // Apply step and compute new objective function.
    // currentStep.set(step);
    // currentStep.scale(stepsize);
    // currentParams.set(parameters);
    // currentParams.add(currentStep);
    //
    // boolean foundObj = false;
    // while(!foundObj)
    // try {
    // currentObj = obj.getValue(currentParams) +
    // getPenaltyFunction(currentParams);
    // damper++;
    // foundObj = true;
    // } catch(OptimizationException e) {
    // stepsize = stepsize / 2;
    // currentStep.set(step);
    // currentStep.scale(stepsize);
    // currentParams.set(parameters);
    // currentParams.add(currentStep);
    // damper += 2;
    // }
    // }
    //
    // // Set the new parameters.
    // parameters = currentParams;
    // currentObjective = currentObj;
    // return true;
    // }

    // Computes quadratic form xT*a*x.
    private double quadraticForm(Vector x, Matrix a)
    {
        Matrix xm = new DenseMatrix(x);
        Matrix xmt = new DenseMatrix(1, x.size());
        xmt = xm.transpose(xmt);
        xmt.mult(a, xmt.copy());
        Matrix result = new DenseMatrix(1, 1);
        result = xmt.mult(xm, result);
        return result.get(0, 0);
    }

    private Vector convertColumnMatrixToVector(Matrix column)
    {
        int n = column.numRows();
        Vector result = new DenseVector(n);
        for (int i = 0; i < n; i++)
            result.set(i, column.get(i, 0));
        return result;
    }

    private Matrix getMarquardtCorrection(Matrix hessian)
    {
        int n = hessian.numRows();
        Matrix result = new BandMatrix(n, 0, 0);
        for (int i = 0; i < n; i++)
        {
            double entry = hessian.get(i, i);
            if (entry == 0) entry = 1;
            result.set(i, i, entry);
        }

        return result;
    }

    public abstract static class EachIterationCallback
    {
        public abstract void finishedIteration(int iteration);
    }

    /**
     * Performs iterations of the Marquardt method until convergence occurs.
     * Convergence is deemed to have occurred if the coefficients in three
     * successive iterations differ by less than <code>epsilon</code>. This
     * method will find the minimum for a single weighting of any penalty
     * functions; use <code>minimize()</code> to do the full solution, including
     * reducing the weighting of the penalty function until the solution is
     * found.
     * 
     * @param epsilon
     *            The threshold at which the optimization is deemed to have
     *            converged. There is a separate epsilon for each coefficient,
     *            so that coefficients with different orders of magnitude can be
     *            handled properly; the epsilons must be given in the same order
     *            as the coefficients.
     * @param maxIterations
     *            The maximum number of iterations - after that number of
     *            iterations, the method will return its results even if it has
     *            not converged.
     * @return The values of the parameters at the optimum.
     */
    public Vector iterateToConvergence(Vector epsilon, int maxIterations,
            EachIterationCallback callBacker)
    {
        iteration = 0;
        Vector previousParameters = parameters;
        boolean lastTwoIterationsConverged = false;
        boolean converged = false;
        logger.info("Starting optimization - max iterations = " + maxIterations);
        obj.logCurrentValues(logger);
        try
        {
            while (!converged && iteration < maxIterations)
            {
                doOneIteration();
                if (callBacker != null) callBacker.finishedIteration(iteration);
                boolean thisIterationConverged = checkConvergence(parameters, previousParameters,
                        epsilon);
                previousParameters = parameters;
                converged = thisIterationConverged && lastTwoIterationsConverged;
                lastTwoIterationsConverged = thisIterationConverged;
            }
            if (iteration < maxIterations) termination = CONVERGED;
            else termination = MAX_ITERATIONS;
        } catch (OptimizationException e)
        {
            termination = INVALID_STEP;
        }

        penaltyIteration++;
        alpha = alpha / 10;

        return parameters.copy();
    }

    private boolean checkConvergence(Vector currentParameters, Vector previousParameters,
            Vector epsilon)
    {
        for (int i = 0; i < currentParameters.size(); i++)
        {
            double diff = Math.abs(currentParameters.get(i) - previousParameters.get(i));
            if (diff >= epsilon.get(i)) return false;
        }
        return true;
    }

    /**
     * !!! Note - this method is not currently supported !!! Finds the minimum
     * of the objective function, subject to the constraints provided using the
     * <code>addConstraint</code> method. This method calls
     * <code>iterateToConvergence</code> repeatedly while making the constraints
     * "looser" until that method's output changes by less than
     * <code>penaltyEpsilon</code> for three consecutive iterations. The methods
     * <code>lastRunConverged</code>, <code>lastRunMaxIterations</code>, and
     * <code>lastRunInvalidStep</code> refer to the termination cause for the
     * last call to <code>iterateToConvergence</code> (with the loosest
     * constraint), since it is that iteration's termination that is relevant
     * for this method (e.g. if the last iteration returns normally, this
     * indicates a valid solution, even if an earlier iteration terminated
     * because of an exception). Note that if no constraints have been added,
     * this method does exactly the same thing as
     * <code>iterateToConvergence</code>.
     * 
     * @param epsilon
     *            The <code>epsilon</code> parameter to pass to
     *            <code>iterateToConvergence</code>.
     * @param maxIterations
     *            The <code>maxIterations</code> parameter to pass to
     *            <code>iterateToConvergence</code>.
     * @param penaltyEpsilon
     *            The threshold at which the minimum is deemed to have been
     *            found.
     * @param penaltyMaxIterations
     *            The maximum number of calls allowed to
     *            <code>iterateToConvergence</code>.
     * @return The values of the parameters at the optimum.
     * @throws OptimizationException
     *             if the current parameters are invalid.
     */
    public Vector minimize(Vector epsilon, int maxIterations, Vector penaltyEpsilon,
            int penaltyMaxIterations, EachIterationCallback callBack) throws OptimizationException
    {
        if (constraints.size() == 0) return iterateToConvergence(epsilon, maxIterations, callBack);

        Vector previousParameters = parameters;
        boolean lastTwoIterationsConverged = false;
        boolean converged = false;
        while (!converged && penaltyIteration < maxIterations)
        {
            reset(previousParameters);
            iterateToConvergence(epsilon, maxIterations, callBack);
            boolean thisIterationConverged = checkConvergence(parameters, previousParameters,
                    penaltyEpsilon);
            previousParameters = parameters;
            converged = thisIterationConverged && lastTwoIterationsConverged;
            lastTwoIterationsConverged = thisIterationConverged;
        }
        if (iteration < maxIterations) penaltyConverged = true;
        else penaltyConverged = false;
        return parameters.copy();
    }

    /**
     * Returns the current value of the objective function after the most recent
     * iteration or reset.
     * 
     * @return The current value of the objective function.
     */
    public double getCurrentObjectiveValue()
    {
        return currentObjective;
    }

    /**
     * Returns the number of iterations performed since the last call to
     * <code>reset()</code>.
     * 
     * @return The number of iterations.
     */
    public int getNumberOfIterations()
    {
        return iteration;
    }

    /**
     * Returns the number of penalty function iterations performed since the
     * last call to <code>resetPenalty()</code>.
     * 
     * @return The number of penalty function iterations.
     */
    public int getNumberOfPenaltyIterations()
    {
        return penaltyIteration;
    }

    /**
     * Determines whether the last execution of
     * <code>iterateToConvergence</code> converged on a solution.
     * 
     * @return True if the optimizer converged on a solution, and false
     *         otherwise (including if <code>iterateToConvergence</code> has
     *         never been invoked.
     */
    public boolean lastRunConverged()
    {
        return termination == CONVERGED;
    }

    /**
     * Determines whether the last execution of
     * <code>iterateToConvergence</code> hit the maximum number of iterations
     * before finding a solution.
     * 
     * @return True if the optimizer ran out of iterations, and false otherwise
     *         (including if <code>iterateToConvergence</code> has never been
     *         invoked.
     */
    public boolean lastRunMaxIterations()
    {
        return termination == MAX_ITERATIONS;
    }

    /**
     * Determines whether the last execution of <code>minimize</code> converged
     * on a solution
     * 
     * @return True of the optimizer converged on a solution, and false
     *         otherwise (including if <code>minimize</code> has never been
     *         invoked.
     */
    public boolean lastRunPenaltyConverged()
    {
        return penaltyConverged;
    }

    /**
     * Determines whether the last execution of <code>minimize</code> hit the
     * maximum number of iterations before finding a solution.
     * 
     * @return True if the optimizer ran out of iterations, and false otherwise
     *         (including if <code>minimize</code> has never been invoked.
     */
    public boolean lastRunPenaltyMaxIterations()
    {
        return !penaltyConverged;
    }

    /**
     * Determines whether the last execution of
     * <code>iterateToConvergence</code> halted because a valid step (i.e. one
     * that decreased the objective function) could not be found. In particular,
     * returns true if the objective function threw an exception for every step
     * tried.
     * 
     * @return True if the optimizer converged on a solution, and false
     *         otherwise (including if <code>iterateToConvergence</code> has
     *         never been invoked.
     */
    public boolean lastRunInvalidStep()
    {
        return termination == INVALID_STEP;
    }
}
