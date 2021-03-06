/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2017, Thomas Pointhuber
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 *
 * Neither the name of the copyright holder nor the names of its
 *  contributors may be used to endorse or promote products derived from
 *  this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package at.pointhi.irbuilder.irbuilder.util;

import java.util.Optional;
import java.util.function.Predicate;

import com.oracle.truffle.llvm.parser.model.functions.FunctionDeclaration;
import com.oracle.truffle.llvm.parser.model.visitors.ModelVisitor;

/**
 * Sometimes we want to find specific objects inside a Model and extract/change it.
 *
 * This class should help us in this regard, to give us some standard implementation which can then
 * be extended by the calle, to do operations on single elements of a model.
 *
 * @param <T>
 */
public abstract class ModelExtractor<T extends Object> implements ModelVisitor {
    private Optional<T> match = Optional.empty();
    private final Predicate<? super T> predicate;

    private ModelExtractor(Predicate<? super T> predicate) {
        this.predicate = predicate;
    }

    /**
     * Executes an arbitrary action when a match is found.
     *
     * @param obj Object which matches our predicate
     */
    public void onMatch(T obj) {
    }

    public final Optional<T> getMatch() {
        return match;
    }

    protected final void onVisit(T obj) {
        if (predicate.test(obj)) {
            if (match.isPresent()) {
                throw new AssertionError("the extractor visitor should only match for a single object!");
            }
            match = Optional.of(obj);

            onMatch(obj);
        }
    }

    public static class FunctionDeclarationExtractor extends ModelExtractor<FunctionDeclaration> {

        public FunctionDeclarationExtractor(Predicate<? super FunctionDeclaration> predicate) {
            super(predicate);
        }

        @Override
        public final void visit(FunctionDeclaration function) {
            onVisit(function);
        }
    }

}
