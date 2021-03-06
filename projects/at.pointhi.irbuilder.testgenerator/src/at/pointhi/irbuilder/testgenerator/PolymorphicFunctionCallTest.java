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
package at.pointhi.irbuilder.testgenerator;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.oracle.truffle.llvm.parser.model.ModelModule;
import com.oracle.truffle.llvm.parser.model.enums.BinaryOperator;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.model.functions.FunctionParameter;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.Instruction;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.test.options.TestOptions;
import at.pointhi.irbuilder.irbuilder.ModelModuleBuilder;
import at.pointhi.irbuilder.irbuilder.SimpleInstrunctionBuilder;
import at.pointhi.irbuilder.irbuilder.util.ConstantUtil;
import at.pointhi.irbuilder.testgenerator.util.IntegerBinaryOperations;
import at.pointhi.irbuilder.testgenerator.util.IntegerBinaryOperations.UndefinedArithmeticResult;

@RunWith(Parameterized.class)
public class PolymorphicFunctionCallTest extends BaseSuite {

    private static final Path SUITE_DIR = Paths.get(TestOptions.PROJECT_ROOT + "/../cache/tests/irbuilder/polymorphic");

    private final PrimitiveType type;
    private final int numberOfCallsites;

    @SuppressWarnings("unused") private final IntegerBinaryOperations binOp;

    public PolymorphicFunctionCallTest(PrimitiveType type, int numberOfCallsites) {
        this.type = type;
        this.numberOfCallsites = numberOfCallsites;
        this.binOp = new IntegerBinaryOperations(type);
    }

    @Override
    public Path getSuiteDir() {
        return SUITE_DIR;
    }

    @Override
    public Path getFilename() {
        return Paths.get(String.format("test_polymorphic_call_%d_%s.ll", numberOfCallsites, type));
    }

    @Parameters(name = "{index}: PolymorphicFunctionCallTest[type={0}, numberOfCallsites={1}]")
    public static Collection<Object[]> data() {
        List<Object[]> parameters = new LinkedList<>();

        for (int i = 1; i < 14; i++) {
            addParameter(parameters, PrimitiveType.I32, i);
        }

        return parameters;
    }

    private static void addParameter(List<Object[]> parameters, PrimitiveType type, int numberOfCallsites) {
        parameters.add(new Object[]{type, numberOfCallsites});
    }

    @Override
    public ModelModule constructModelModule() throws UndefinedArithmeticResult {
        ModelModuleBuilder builder = new ModelModuleBuilder();

        final FunctionDefinition[] callSites = new FunctionDefinition[numberOfCallsites];
        for (int i = 0; i < numberOfCallsites; i++) {
            callSites[i] = createCallSite(builder, i);
        }

        createMain(builder, callSites);

        return builder.getModelModule();
    }

    @SuppressWarnings("static-method")
    private void createMain(ModelModuleBuilder builder, @SuppressWarnings("unused") FunctionDefinition[] callSites) {
        FunctionDefinition main = builder.createFunctionDefinition("main", 1, new FunctionType(PrimitiveType.I32, new Type[]{}, false));
        SimpleInstrunctionBuilder instr = new SimpleInstrunctionBuilder(builder, main);

        instr.returnx(ConstantUtil.getI32Const(1)); // 0=OK, 1=ERROR
    }

    private FunctionDefinition createCallSite(ModelModuleBuilder builder, int idx) {
        final String name = "foo_" + idx;
        final FunctionDefinition foo = builder.createFunctionDefinition(name, 1, new FunctionType(type, new Type[]{type, type}, false));
        final SimpleInstrunctionBuilder instr = new SimpleInstrunctionBuilder(builder, foo);

        final FunctionParameter lhs = instr.nextParameter();
        final FunctionParameter rhs = instr.nextParameter();

        final BinaryOperator op = getOperator(idx);
        final Instruction res = instr.binaryOperator(op, lhs, rhs);

        instr.returnx(res);

        return foo;
    }

    private static BinaryOperator getOperator(int idx) {
        final int opIdx = idx % (BinaryOperator.INT_XOR.ordinal() + 1);
        return BinaryOperator.values()[opIdx];
    }

}
