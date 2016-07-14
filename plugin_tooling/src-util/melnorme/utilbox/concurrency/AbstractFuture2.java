/*******************************************************************************
 * Copyright (c) 2016 Bruno Medeiros and other Contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bruno Medeiros - initial API and implementation
 *******************************************************************************/
package melnorme.utilbox.concurrency;

import static melnorme.utilbox.core.Assert.AssertNamespace.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class AbstractFuture2<RET> implements Future2<RET> {
	
	protected final CompletableResult<RET> completableResult = new CompletableResult<>();
	
	public AbstractFuture2() {
		super();
	}
	
	@Override
	public boolean isCancelled() {
		return completableResult.isCancelled();
	}
	
	@Override
	public boolean isDone() {
		return completableResult.isDone();
	}
	
	@Override
	public RET awaitResult() throws InterruptedException, OperationCancellation {
		return completableResult.awaitResult();
	}
	
	@Override
	public RET awaitResult(long timeout, TimeUnit unit) 
			throws InterruptedException, TimeoutException, OperationCancellation {
		return completableResult.awaitResult(timeout, unit);
	}
	
	/* -----------------  ----------------- */ 
	
	public Future<RET> asFuture1() {
		return asFuture;
	}
	
	protected final Future<RET> asFuture = new Future<RET>() {
		
		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return AbstractFuture2.this.tryCancel();
		}
		
		@Override
		public boolean isCancelled() {
			return AbstractFuture2.this.isCancelled();
		}
		
		@Override
		public boolean isDone() {
			return AbstractFuture2.this.isDone();
		}
		
		@Override
		public RET get() throws InterruptedException, ExecutionException {
			try {
				return awaitResult();
			} catch(Throwable e) {
				// Don't throw java.util.concurrent.CancellationException because it is a RuntimeException
				throw toExecutionException(e);
			}
		}
		@Override
		public RET get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			try {
				return awaitResult(timeout, unit);
			} catch(Throwable e) {
				// Don't throw java.util.concurrent.CancellationException because it is a RuntimeException
				throw toExecutionException(e);
			}
		}
		
		public ExecutionException toExecutionException(Throwable e) throws ExecutionException {
			return new ExecutionException(e);
		}
		
	};
	
	/* -----------------  ----------------- */
	
	public static class CompletedFuture<RET> extends AbstractFuture2<RET> {
		
		public CompletedFuture(RET result) {
			completableResult.setResult(result);
			assertTrue(isCompletedWithResult());
		}
		
		@Override
		public boolean tryCancel() {
			return false;
		}
	}
	
}