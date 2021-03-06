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

package at.pointhi.irbuilder.irwriter.visitors.instruction;

import com.oracle.truffle.llvm.parser.model.blocks.InstructionBlock;
import com.oracle.truffle.llvm.parser.model.enums.AtomicOrdering;
import com.oracle.truffle.llvm.parser.model.enums.Flag;
import com.oracle.truffle.llvm.parser.model.enums.SynchronizationScope;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDeclaration;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.model.functions.FunctionParameter;
import com.oracle.truffle.llvm.parser.model.symbols.constants.Constant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.integer.IntegerConstant;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.AllocateInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.BinaryOperationInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.BranchInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.Call;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.CallInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.CastInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.CompareExchangeInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.CompareInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.ConditionalBranchInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.ExtractElementInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.ExtractValueInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.FenceInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.GetElementPointerInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.IndirectBranchInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.InsertElementInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.InsertValueInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.Instruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.Invoke;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.InvokeInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.LandingpadInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.LoadInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.PhiInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.ReadModifyWriteInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.ResumeInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.ReturnInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.SelectInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.ShuffleVectorInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.StoreInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.SwitchInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.SwitchOldInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.UnreachableInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.VoidCallInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.VoidInvokeInstruction;
import com.oracle.truffle.llvm.parser.model.visitors.InstructionVisitor;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.MetaType;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.VoidType;
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;
import com.oracle.truffle.llvm.runtime.types.symbols.ValueSymbol;

import at.pointhi.irbuilder.irwriter.IRWriter;
import at.pointhi.irbuilder.irwriter.IRWriterVersion;
import at.pointhi.irbuilder.irwriter.visitors.IRWriterBaseVisitor;

public class IRWriterInstructionVisitor extends IRWriterBaseVisitor implements InstructionVisitor {

    public IRWriterInstructionVisitor(IRWriterVersion.IRWriterVisitors visitors, IRWriter.PrintTarget target) {
        super(visitors, target);
    }

    protected static final String LLVMIR_LABEL_ALIGN = "align";

    private static final String INDENTATION = "  ";
    private static final String DEEP_INDENTATION = "        ";

    protected void writeIndent() {
        write(INDENTATION);
    }

    protected void writeDeepIndent() {
        write(INDENTATION);
        write(DEEP_INDENTATION);
    }

    protected void writeInstructionTail(@SuppressWarnings("unused") Instruction instr) {
        writeln();
    }

    private static final String LLVMIR_LABEL_ALLOCATE = "alloca";

    @Override
    public void visit(AllocateInstruction allocate) {
        writeIndent();

        // <result> = alloca <type>
        writef("%s = %s ", allocate.getName(), LLVMIR_LABEL_ALLOCATE);
        writeType(allocate.getPointeeType());

        // [, <ty> <NumElements>]
        if (!(allocate.getCount() instanceof IntegerConstant && ((IntegerConstant) allocate.getCount()).getValue() == 1)) {
            write(", ");
            writeType(allocate.getCount().getType());
            write(" ");
            writeInnerSymbolValue(allocate.getCount());
        }

        // [, align <alignment>]
        if (allocate.getAlign() != 0) {
            writef(", %s %d", LLVMIR_LABEL_ALIGN, 1 << (allocate.getAlign() - 1));
        }

        writeInstructionTail(allocate);
    }

    @Override
    public void visit(BinaryOperationInstruction operation) {
        writeIndent();

        // <result> = <op>
        writef("%s = %s ", operation.getName(), operation.getOperator().getIrString());

        // { <flag>}*
        for (Flag flag : operation.getFlags()) {
            write(flag.getIrString());
            write(" ");
        }

        // <ty> <op1>, <op2>
        writeType(operation.getType());
        write(" ");
        writeInnerSymbolValue(operation.getLHS());
        write(", ");
        writeInnerSymbolValue(operation.getRHS());

        writeInstructionTail(operation);
    }

    private static final String LLVMIR_LABEL_BRANCH = "br";

    private static final String LLVMIR_LABEL_BRANCH_LABEL = "label";

    @Override
    public void visit(BranchInstruction branch) {
        writeIndent();

        writef("%s %s ", LLVMIR_LABEL_BRANCH, LLVMIR_LABEL_BRANCH_LABEL);
        writeBlockName(branch.getSuccessor());

        writeInstructionTail(branch);
    }

    protected static final String LLVMIR_LABEL_CALL = "call";

    @Override
    public void visit(CallInstruction call) {
        writeIndent();

        // <result> =
        write(call.getName());
        write(" = ");

        // [tail] call
        write(LLVMIR_LABEL_CALL);
        write(" ");

        writeFunctionCall(call);

        writeInstructionTail(call);
    }

    @Override
    public void visit(CastInstruction cast) {
        writeIndent();

        writef("%s = %s ", cast.getName(), cast.getOperator().getIrString());
        writeType(cast.getValue().getType());
        write(" ");
        writeInnerSymbolValue(cast.getValue());
        write(" to ");
        writeType(cast.getType());

        writeInstructionTail(cast);
    }

    protected static final String LLVMIR_LABEL_COMPARE_EXCHANGE = "cmpxchg";

    /*
     * @see http://releases.llvm.org/3.2/docs/LangRef.html#i_cmpxchg
     */
    @Override
    public void visit(CompareExchangeInstruction cmpxchg) {
        writeIndent();

        write(cmpxchg.getName());
        write(" = ");
        write(LLVMIR_LABEL_COMPARE_EXCHANGE);

        if (cmpxchg.isVolatile()) {
            write(" volatile");
        }

        write(" ");
        writeType(cmpxchg.getPtr().getType());
        write(" ");
        writeInnerSymbolValue(cmpxchg.getPtr());

        write(", ");
        writeSymbolType(cmpxchg.getCmp());
        write(" ");
        writeInnerSymbolValue(cmpxchg.getCmp());

        write(", ");
        writeSymbolType(cmpxchg.getReplace());
        write(" ");
        writeInnerSymbolValue(cmpxchg.getReplace());

        if (cmpxchg.getSynchronizationScope().equals(SynchronizationScope.SINGLE_THREAD)) {
            write(" singlethread");
        }

        write(" ");
        write(cmpxchg.getSuccessOrdering().getIrString());

        writeInstructionTail(cmpxchg);
    }

    private static final String LLVMIR_LABEL_COMPARE = "icmp";
    private static final String LLVMIR_LABEL_COMPARE_FP = "fcmp";

    @Override
    public void visit(CompareInstruction operation) {
        writeIndent();

        // <result> = <icmp|fcmp> <cond> <ty> <op1>, <op2>
        write(operation.getName());
        write(" = ");

        if (operation.getOperator().isFloatingPoint()) {
            write(LLVMIR_LABEL_COMPARE_FP);
        } else {
            write(LLVMIR_LABEL_COMPARE);
        }

        write(" ");
        write(operation.getOperator().getIrString());
        write(" ");
        writeType(operation.getLHS().getType());
        write(" ");
        writeInnerSymbolValue(operation.getLHS());
        write(", ");
        writeInnerSymbolValue(operation.getRHS());

        writeInstructionTail(operation);
    }

    private static final String LLVMIR_LABEL_CONDITIONAL_BRANCH = "br";

    @Override
    public void visit(ConditionalBranchInstruction branch) {
        writeIndent();

        // br i1 <cond>, label <iftrue>, label <iffalse>
        write(LLVMIR_LABEL_CONDITIONAL_BRANCH);
        write(" ");
        writeType(branch.getCondition().getType());
        write(" ");
        writeInnerSymbolValue(branch.getCondition());
        write(", ");
        write(LLVMIR_LABEL_BRANCH_LABEL);
        write(" ");
        writeBlockName(branch.getTrueSuccessor());
        write(", ");
        write(LLVMIR_LABEL_BRANCH_LABEL);
        write(" ");
        writeBlockName(branch.getFalseSuccessor());

        writeInstructionTail(branch);
    }

    private static final String LLVMIR_LABEL_EXTRACT_ELEMENT = "extractelement";

    @Override
    public void visit(ExtractElementInstruction extract) {
        writeIndent();

        // <result> = extractelement <n x <ty>> <val>, i32 <idx>
        write(extract.getName());
        write(" = ");
        write(LLVMIR_LABEL_EXTRACT_ELEMENT);
        write(" ");
        writeType(extract.getVector().getType());
        write(" ");
        writeInnerSymbolValue(extract.getVector());
        write(", ");
        writeType(extract.getIndex().getType());
        write(" ");
        writeInnerSymbolValue(extract.getIndex());

        writeInstructionTail(extract);
    }

    protected static final String LLVMIR_LABEL_EXTRACT_VALUE = "extractvalue";

    @Override
    public void visit(ExtractValueInstruction extract) {
        writeIndent();

        /*
         * TODO: not perfect, but should detect most of the hacked occurrences for
         * CompareExchangeInstructions correctly, but only works with LLVM 3.2 correctly.
         *
         * This code is required, because Sulong is inserting a ExtractValueInstruction after a
         * CompareExchangeInstruction, which would be invalid LLVM IR in our case.
         */
        if (extract.getAggregate() instanceof CompareExchangeInstruction) {
            writeAssignment(extract.getAggregate(), extract, extract.getType());

            // write original instruction as comment
            write(" ;");
        }

        // <result> = extractvalue <aggregate type> <val>, <idx>{, <idx>}*
        write(extract.getName());
        write(" = ");
        write(LLVMIR_LABEL_EXTRACT_VALUE);
        write(" ");
        writeType(extract.getAggregate().getType());
        write(" ");
        writeInnerSymbolValue(extract.getAggregate());
        writef(", %d", extract.getIndex());

        writeInstructionTail(extract);
    }

    protected static final String LLVMIR_LABEL_GET_ELEMENT_POINTER = "getelementptr";

    protected static final String LLVMIR_LABEL_GET_ELEMENT_POINTER_INBOUNDS = "inbounds";

    @Override
    public void visit(GetElementPointerInstruction gep) {
        writeIndent();

        // <result> = getelementptr
        writef("%s = %s ", gep.getName(), LLVMIR_LABEL_GET_ELEMENT_POINTER);

        // [inbounds]
        if (gep.isInbounds()) {
            write(LLVMIR_LABEL_GET_ELEMENT_POINTER_INBOUNDS);
            write(" ");
        }

        // <pty>* <ptrval>
        writeType(gep.getBasePointer().getType());
        write(" ");
        writeInnerSymbolValue(gep.getBasePointer());

        // {, <ty> <idx>}*
        for (final Symbol sym : gep.getIndices()) {
            write(", ");
            writeType(sym.getType());
            write(" ");
            writeInnerSymbolValue(sym);
        }

        writeInstructionTail(gep);
    }

    protected static final String LLVMIR_LABEL_FENCE = "fence";

    @Override
    public void visit(FenceInstruction fence) {
        writeIndent();

        write(LLVMIR_LABEL_FENCE);
        write(" ");

        if (fence.getSynchronizationScope() != null) {
            // TODO: output IR string
            // write(fence.getSynchronizationScope());
            // write(" ");
        }

        write(fence.getAtomicOrdering().getIrString());

        writeInstructionTail(fence);
    }

    private static final String LLVMIR_LABEL_INDIRECT_BRANCH = "indirectbr";

    @Override
    public void visit(IndirectBranchInstruction branch) {
        writeIndent();

        // indirectbr <somety>* <address>, [ label <dest1>, label <dest2>, ... ]
        write(LLVMIR_LABEL_INDIRECT_BRANCH);
        write(" ");
        writeType(branch.getAddress().getType());
        write(" ");
        writeInnerSymbolValue(branch.getAddress());
        write(", [ ");
        for (int i = 0; i < branch.getSuccessorCount(); i++) {
            if (i != 0) {
                write(", ");
            }
            write(LLVMIR_LABEL_BRANCH_LABEL);
            write(" ");
            writeBlockName(branch.getSuccessor(i));
        }
        write(" ]");

        writeInstructionTail(branch);
    }

    private static final String LLVMIR_LABEL_INSERT_ELEMENT = "insertelement";

    @Override
    public void visit(InsertElementInstruction insert) {
        writeIndent();

        // <result> = insertelement <n x <ty>> <val>, <ty> <elt>, i32 <idx>
        write(insert.getName());
        write(" = ");
        write(LLVMIR_LABEL_INSERT_ELEMENT);
        write(" ");
        writeType(insert.getVector().getType());
        write(" ");
        writeInnerSymbolValue(insert.getVector());
        write(", ");
        writeType(insert.getValue().getType());
        write(" ");
        writeInnerSymbolValue(insert.getValue());
        write(", ");
        writeType(insert.getIndex().getType());
        write(" ");
        writeInnerSymbolValue(insert.getIndex());

        writeInstructionTail(insert);
    }

    private static final String LLVMIR_LABEL_INSERT_VALUE = "insertvalue";

    @Override
    public void visit(InsertValueInstruction insert) {
        writeIndent();

        // <result> = insertvalue <aggregate type> <val>, <ty> <elt>, <idx>{, <idx>}*
        write(insert.getName());
        write(" = ");
        write(LLVMIR_LABEL_INSERT_VALUE);
        write(" ");
        writeType(insert.getAggregate().getType());
        write(" ");
        writeInnerSymbolValue(insert.getAggregate());
        write(", ");
        writeType(insert.getValue().getType());
        write(" ");
        writeInnerSymbolValue(insert.getValue());
        writef(", %d", insert.getIndex());

        writeInstructionTail(insert);
    }

    protected static final String LLVMIR_LABEL_LOAD = "load";

    protected static final String LLVMIR_LABEL_ATOMIC = "atomic";

    protected static final String LLVMIR_LABEL_VOLATILE = "volatile";

    protected static final String LLVMIR_LABEL_SINGLETHREAD = "singlethread";

    @Override
    public void visit(LoadInstruction load) {
        writeIndent();

        write(load.getName());
        write(" = ");
        write(LLVMIR_LABEL_LOAD);

        if (load.getAtomicOrdering() != AtomicOrdering.NOT_ATOMIC) {
            write(" ");
            write(LLVMIR_LABEL_ATOMIC);
        }

        if (load.isVolatile()) {
            write(" ");
            write(LLVMIR_LABEL_VOLATILE);
        }

        write(" ");
        writeType(load.getSource().getType());

        write(" ");
        writeInnerSymbolValue(load.getSource());

        if (load.getAtomicOrdering() != AtomicOrdering.NOT_ATOMIC) {
            if (load.getSynchronizationScope() == SynchronizationScope.SINGLE_THREAD) {
                write(" ");
                write(LLVMIR_LABEL_SINGLETHREAD);
            }

            write(" ");
            write(load.getAtomicOrdering().getIrString());
        }

        if (load.getAlign() != 0) {
            writef(", %s %d", LLVMIR_LABEL_ALIGN, 1 << (load.getAlign() - 1));
        }

        writeInstructionTail(load);
    }

    private static final String LLVMIR_LABEL_PHI = "phi";

    @Override
    public void visit(PhiInstruction phi) {
        writeIndent();

        // <result> = phi <ty>
        write(phi.getName());
        write(" = ");
        write(LLVMIR_LABEL_PHI);
        write(" ");

        writeType(phi.getType());
        write(" ");

        // [ <val0>, <label0>], ...
        for (int i = 0; i < phi.getSize(); i++) {
            if (i != 0) {
                write(", ");
            }

            write("[ ");
            writeInnerSymbolValue(phi.getValue(i));
            write(", ");
            writeBlockName(phi.getBlock(i));
            write(" ]");
        }

        writeInstructionTail(phi);
    }

    private static final String LLVMIR_LABEL_RETURN = "ret";

    @Override
    public void visit(ReturnInstruction ret) {
        writeIndent();

        write(LLVMIR_LABEL_RETURN);
        write(" ");

        final Symbol value = ret.getValue();
        if (value == null) {
            writeType(VoidType.INSTANCE);
        } else {
            writeSymbolType(value);
            write(" ");
            writeInnerSymbolValue(value);
        }

        writeInstructionTail(ret);
    }

    private static final String LLVMIR_LABEL_SELECT = "select";

    @Override
    public void visit(SelectInstruction select) {
        writeIndent();

        // <result> = select selty <cond>, <ty> <val1>, <ty> <val2>
        write(select.getName());
        write(" = ");
        write(LLVMIR_LABEL_SELECT);
        write(" ");

        writeType(select.getCondition().getType());
        write(" ");
        writeInnerSymbolValue(select.getCondition());
        write(", ");

        writeSymbolType(select.getTrueValue());
        write(" ");
        writeInnerSymbolValue(select.getTrueValue());
        write(", ");

        writeSymbolType(select.getFalseValue());
        write(" ");
        writeInnerSymbolValue(select.getFalseValue());

        writeInstructionTail(select);
    }

    private static final String LLVMIR_LABEL_SHUFFLE_VECTOR = "shufflevector";

    @Override
    public void visit(ShuffleVectorInstruction shuffle) {
        writeIndent();

        // <result> = shufflevector <n x <ty>> <v1>, <n x <ty>> <v2>, <m x i32> <mask>
        write(shuffle.getName());
        write(" = ");
        write(LLVMIR_LABEL_SHUFFLE_VECTOR);
        write(" ");

        writeType(shuffle.getVector1().getType());
        write(" ");
        writeInnerSymbolValue(shuffle.getVector1());
        write(", ");

        writeType(shuffle.getVector2().getType());
        write(" ");
        writeInnerSymbolValue(shuffle.getVector2());
        write(", ");

        writeType(shuffle.getMask().getType());
        write(" ");
        writeInnerSymbolValue(shuffle.getMask());

        writeInstructionTail(shuffle);
    }

    private static final String LLVMIR_LABEL_STORE = "store";

    @Override
    public void visit(StoreInstruction store) {
        writeIndent();

        writef("%s ", LLVMIR_LABEL_STORE);

        if (store.getAtomicOrdering() != AtomicOrdering.NOT_ATOMIC) {
            write(LLVMIR_LABEL_ATOMIC);
            write(" ");
        }

        if (store.isVolatile()) {
            write(LLVMIR_LABEL_VOLATILE);
            write(" ");
        }

        writeType(((PointerType) store.getDestination().getType()).getPointeeType());
        write(" ");

        writeInnerSymbolValue(store.getSource());
        write(", ");

        writeType(store.getDestination().getType());
        write(" ");
        writeInnerSymbolValue(store.getDestination());

        if (store.getAtomicOrdering() != AtomicOrdering.NOT_ATOMIC) {
            if (store.getSynchronizationScope() == SynchronizationScope.SINGLE_THREAD) {
                write(" ");
                write(LLVMIR_LABEL_SINGLETHREAD);
            }

            write(" ");
            write(store.getAtomicOrdering().getIrString());
        }

        if (store.getAlign() != 0) {
            writef(", %s %d", LLVMIR_LABEL_ALIGN, 1 << (store.getAlign() - 1));
        }

        writeInstructionTail(store);
    }

    private static final String LLVMIR_LABEL_SWITCH = "switch";

    @Override
    public void visit(SwitchInstruction select) {
        writeIndent();

        // switch <intty> <value>, label <defaultdest>
        write(LLVMIR_LABEL_SWITCH);
        write(" ");
        writeType(select.getCondition().getType());
        write(" ");
        writeInnerSymbolValue(select.getCondition());
        write(", ");
        write(LLVMIR_LABEL_BRANCH_LABEL);
        write(" ");
        writeBlockName(select.getDefaultBlock());

        write(" [ ");
        for (int i = 0; i < select.getCaseCount(); i++) {
            if (i != 0) {
                writeln();
                writeIndent();
                writeIndent();
            }

            final Symbol val = select.getCaseValue(i);
            final InstructionBlock blk = select.getCaseBlock(i);
            writeType(val.getType());
            write(" ");
            writeInnerSymbolValue(val);
            write(", ");
            write(LLVMIR_LABEL_BRANCH_LABEL);
            write(" ");
            writeBlockName(blk);
        }
        write(" ]");

        writeInstructionTail(select);
    }

    private static final String LLVMIR_LABEL_SWITCH_OLD = "switch";

    @Override
    public void visit(SwitchOldInstruction select) {
        writeIndent();

        // switch <intty> <value>, label <defaultdest>
        write(LLVMIR_LABEL_SWITCH_OLD);
        write(" ");
        writeType(select.getCondition().getType());
        write(" ");
        writeInnerSymbolValue(select.getCondition());
        write(", ");
        write(LLVMIR_LABEL_BRANCH_LABEL);
        write(" ");
        writeBlockName(select.getDefaultBlock());

        write(" [ ");
        for (int i = 0; i < select.getCaseCount(); i++) {
            if (i != 0) {
                writeln();
                writeIndent();
                writeIndent();
            }

            writeType(select.getCondition().getType());
            write(String.format(" %d, ", select.getCaseValue(i)));
            write(LLVMIR_LABEL_BRANCH_LABEL);
            write(" ");
            writeBlockName(select.getCaseBlock(i));
        }
        write(" ]");

        writeInstructionTail(select);
    }

    private static final String LLVMIR_LABEL_UNREACHABLE = "unreachable";

    @Override
    public void visit(UnreachableInstruction unreachable) {
        writeIndent();

        write(LLVMIR_LABEL_UNREACHABLE);

        writeInstructionTail(unreachable);
    }

    @Override
    public void visit(VoidCallInstruction call) {
        writeIndent();

        // [tail] call
        write(LLVMIR_LABEL_CALL);
        write(" ");

        writeFunctionCall(call);

        writeInstructionTail(call);
    }

    private static final String LLVMIR_LABEL_INVOKE = "invoke";

    @Override
    public void visit(InvokeInstruction invoke) {
        // <result> =
        write(invoke.getName());
        write(" = ");

        // invoke
        write(LLVMIR_LABEL_INVOKE);
        write(" ");

        writeFunctionInvoke(invoke);

        writeInstructionTail(invoke);
    }

    @Override
    public void visit(VoidInvokeInstruction invoke) {
        writeIndent();

        // invoke
        write(LLVMIR_LABEL_INVOKE);
        write(" ");

        writeFunctionInvoke(invoke);

        writeInstructionTail(invoke);
    }

    private static final String LLVMIR_LABEL_LANDINGPAD = "landingpad";

    private static final String LLVMIR_LABEL_LANDINGPAD_CLEANUP = "cleanup";

    @Override
    public void visit(LandingpadInstruction landingpad) {
        writeIndent();

        // <resultval> =
        write(landingpad.getName());
        write(" = ");

        // landingpad
        write(LLVMIR_LABEL_LANDINGPAD);
        write(" ");

        // <resultty>
        writeType(landingpad.getType());

        // [ cleanup ]
        if (landingpad.isCleanup()) {
            writeln();
            writeDeepIndent();
            write(LLVMIR_LABEL_LANDINGPAD_CLEANUP);

        }

        // <clause>*
        // TODO: implement

        writeInstructionTail(landingpad);
    }

    private static final String LLVMIR_LABEL_RESUME = "resume";

    @Override
    public void visit(ResumeInstruction resume) {
        writeIndent();

        write(LLVMIR_LABEL_RESUME);
        write(" ");

        // writeSymbolType(resume.getValue());
        writeSymbolType(resume.getValue());
        write(" ");
        writeInnerSymbolValue(resume.getValue());

        writeInstructionTail(resume);
    }

    private static final String LLVMIR_LABEL_ATOMICRMW = "atomicrmw";

    @Override
    public void visit(ReadModifyWriteInstruction rmw) {
        writeIndent();

        write(LLVMIR_LABEL_ATOMICRMW);
        write(" ");

        if (rmw.isVolatile()) {
            write("volatile ");
        }

        write(rmw.getOperator().toString().toLowerCase()); // TODO: implement getIrString();
        write(" ");

        writeSymbolType(rmw.getPtr());
        write(" ");
        writeInnerSymbolValue(rmw.getPtr());
        write(", ");

        writeSymbolType(rmw.getValue());
        write(" ");
        writeInnerSymbolValue(rmw.getValue());
        write(" ");

        if (rmw.getSynchronizationScope() != null) {
            // write(rmw.getSynchronizationScope().getIrString()); // TODO: implement
            // write(" ");
        }

        write(rmw.getAtomicOrdering().getIrString());

        writeInstructionTail(rmw);
    }

    /**
     * @see <a href="http://releases.llvm.org/3.2/docs/LangRef.html#i_call">LangRef</a>
     */
    protected void writeFunctionCall(Call call) {
        // [cconv] [ret attrs]

        /*
         * <ty>
         *
         * 'ty': the type of the call instruction itself which is also the type of the return value.
         * Functions that return no value are marked void.
         */
        writeType(getSymbolType(call));
        write(" ");

        /*
         * [<fnty>*]
         *
         * 'fnty': shall be the signature of the pointer to function value being invoked. The
         * argument types must match the types implied by this signature. This type can be omitted
         * if the function is not varargs and if the function type does not return a pointer to a
         * function.
         */
        final FunctionType funcType = getFunctionType(call.getCallTarget());
        final Type retType = funcType.getReturnType();
        if (funcType.isVarargs() || (retType instanceof PointerType && ((PointerType) retType).getPointeeType() instanceof FunctionType)) {
            writeFormalArguments(funcType);
            write("*");
            write(" ");
        }

        /*
         * <fnptrval>
         *
         * 'fnptrval': An LLVM value containing a pointer to a function to be invoked. In most
         * cases, this is a direct function invocation, but indirect calls are just as possible,
         * calling an arbitrary pointer to function value.
         */
        writeInnerSymbolValue(call.getCallTarget());

        /*
         * (<function args>)
         *
         * 'function args': argument list whose types match the function signature argument types
         * and parameter attributes. All arguments must be of first class type. If the function
         * signature indicates the function accepts a variable number of arguments, the extra
         * arguments can be specified.
         */
        writeActualArgs(call);

        // [fn attrs]
    }

    protected void writeFunctionInvoke(Invoke call) {
        writeType(getSymbolType(call));
        write(" ");

        final FunctionType funcType = getFunctionType(call.getCallTarget());
        final Type retType = funcType.getReturnType();
        if (funcType.isVarargs() || (retType instanceof PointerType && ((PointerType) retType).getPointeeType() instanceof FunctionType)) {
            writeFormalArguments(funcType);
            write("*");
            write(" ");
        }

        writeInnerSymbolValue(call.getCallTarget());

        writeActualArgs(call);

        writeln();
        writeDeepIndent();

        write("to label ");
        writeBlockName(call.normalSuccessor());

        write(" unwind label ");
        writeBlockName(call.unwindSuccessor());
    }

    protected static Type getSymbolType(Call call) {
        if (call instanceof VoidCallInstruction) {
            return ((VoidCallInstruction) call).getType();
        } else if (call instanceof CallInstruction) {
            return ((CallInstruction) call).getType();
        } else {
            throw new RuntimeException("unexpected type");
        }
    }

    protected static Type getSymbolType(Invoke call) {
        if (call instanceof VoidInvokeInstruction) {
            return ((VoidInvokeInstruction) call).getType();
        } else if (call instanceof InvokeInstruction) {
            return ((InvokeInstruction) call).getType();
        } else {
            throw new RuntimeException("unexpected type");
        }
    }

    protected static FunctionType getFunctionType(Symbol callTarget) {
        if (callTarget instanceof FunctionDeclaration) {
            return ((FunctionDeclaration) callTarget).getType();
        } else if (callTarget instanceof FunctionDefinition) {
            return ((FunctionDefinition) callTarget).getType();
        } else if (callTarget instanceof FunctionParameter) {
            Type targetType = ((FunctionParameter) callTarget).getType();

            while (targetType instanceof PointerType) {
                targetType = ((PointerType) targetType).getPointeeType();
            }

            if (targetType instanceof FunctionType) {
                return (FunctionType) targetType;
            } else {
                throw new AssertionError("unexpected target type: " + targetType.getClass().getName());
            }
        } else if (callTarget instanceof CallInstruction) {
            Type targetType = ((CallInstruction) callTarget).getType();
            targetType = ((PointerType) targetType).getPointeeType(); // TODO: check
            if (targetType instanceof FunctionType) {
                return (FunctionType) targetType;
            } else {
                throw new AssertionError("unexpected target type: " + targetType.getClass().getName());
            }
        } else if (callTarget instanceof ValueSymbol) {
            Type targetType;
            if (callTarget instanceof LoadInstruction) {
                targetType = ((LoadInstruction) callTarget).getSource().getType();
            } else {
                targetType = callTarget.getType();
            }

            while (targetType instanceof PointerType) {
                targetType = ((PointerType) targetType).getPointeeType();
            }

            if (targetType instanceof FunctionType) {
                return (FunctionType) targetType;
            } else {
                throw new AssertionError("unexpected target type: " + targetType.getClass().getName());
            }
        } else if (callTarget instanceof Constant) {
            Type targetType = callTarget.getType();
            targetType = ((PointerType) targetType).getPointeeType(); // TODO: check
            if (targetType instanceof FunctionType) {
                return (FunctionType) targetType;
            } else {
                throw new AssertionError("unexpected target type: " + targetType.getClass().getName());
            }
        } else {
            throw new AssertionError("unexpected target type: " + callTarget.getClass().getName());
        }
    }

    protected void writeActualArgs(Call call) {
        write("(");
        for (int i = 0; i < call.getArgumentCount(); i++) {
            final Symbol arg = call.getArgument(i);

            if (i != 0) {
                write(", ");
            }

            // At least for @llvm.dbg.declare we need to handle type mismatches
            final FunctionType funcType = getFunctionType(call.getCallTarget());
            final Type funcTypeArgType = i < funcType.getArgumentTypes().length ? funcType.getArgumentTypes()[i] : null;
            if (funcTypeArgType != null && !isEquivalentIRType(funcTypeArgType, getSymbolType(arg))) {
                writeType(funcTypeArgType);
                write(" ");
            }

            writeSymbolType(arg);
            writeAttributesGroupIfPresent(call.getParameterAttributesGroup(i));
            write(" ");
            writeInnerSymbolValue(arg);
        }
        write(")");
    }

    private static boolean isMetadataType(Type type) {
        return type == MetaType.METADATA || type == MetaType.DEBUG;
    }

    private static boolean isEquivalentIRType(Type type1, Type type2) {
        if (type1.equals(type2)) {
            return true;
        }
        // both are declared as metadata
        if (isMetadataType(type1) && isMetadataType(type2)) {
            return true;
        }
        // in case of PrimitiveType, constant and non constant type are not equal by default
        if (type1 instanceof PrimitiveType && type2 instanceof PrimitiveType) {
            if (((PrimitiveType) type1).getPrimitiveKind().equals(((PrimitiveType) type2).getPrimitiveKind())) {
                return true;
            }
        }
        return false;
    }

    protected void writeActualArgs(Invoke call) {
        write("(");
        for (int i = 0; i < call.getArgumentCount(); i++) {
            final Symbol arg = call.getArgument(i);

            if (i != 0) {
                write(", ");
            }

            writeSymbolType(arg);
            writeAttributesGroupIfPresent(call.getParameterAttributesGroup(i));
            write(" ");
            writeInnerSymbolValue(arg);
        }
        write(")");
    }

    /**
     * Write LLVM IR which would be equivalent to an assignment.
     *
     * @param from variable we want to assign
     * @param to resulting variable where we want to place the result
     * @param type type of the variable which is assigned
     */
    protected void writeAssignment(Symbol from, ValueSymbol to, Type type) {
        final String tmpPtrName = "%irwriter_tmp_" + to.getName().substring(1);
        final PointerType tmpPointer = new PointerType(type);

        write(tmpPtrName);
        write(" = ");
        write(LLVMIR_LABEL_ALLOCATE);
        write(" ");
        writeType(type);
        writeln();

        writeIndent();
        write(LLVMIR_LABEL_STORE);
        write(" ");
        writeType(type);
        write(" ");
        writeInnerSymbolValue(from);
        write(", ");
        writeType(tmpPointer);
        write(" ");
        write(tmpPtrName);
        writeln();

        writeIndent();
        write(to.getName());
        write(" = ");
        write(LLVMIR_LABEL_LOAD);
        write(" ");
        writeType(tmpPointer);
        write(" ");
        write(tmpPtrName);
    }

}
