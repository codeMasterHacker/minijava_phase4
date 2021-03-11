import cs132.util.*;
import cs132.util.ProblemException;
import cs132.util.IndentPrinter;

import cs132.vapor.parser.*;
import cs132.vapor.parser.VaporParser;
import cs132.vapor.ast.*;
import cs132.vapor.ast.Node;
import cs132.vapor.ast.VAddr;
import cs132.vapor.ast.VAddr.Label;
import cs132.vapor.ast.VAddr.Var;
import cs132.vapor.ast.VaporProgram;
import cs132.vapor.ast.VAssign;
import cs132.vapor.ast.VBranch;
import cs132.vapor.ast.VBuiltIn;
import cs132.vapor.ast.VBuiltIn.Op;
import cs132.vapor.ast.VCall;
import cs132.vapor.ast.VCodeLabel;
import cs132.vapor.ast.VDataSegment;
import cs132.vapor.ast.VFunction;
import cs132.vapor.ast.VFunction.Stack;
import cs132.vapor.ast.VGoto;
import cs132.vapor.ast.VInstr;
import cs132.vapor.ast.VInstr.Visitor;
import cs132.vapor.ast.VInstr.VisitorP;
import cs132.vapor.ast.VInstr.VisitorPR;
import cs132.vapor.ast.VInstr.VisitorR;
import cs132.vapor.ast.VLabelRef;
import cs132.vapor.ast.VLitInt;
import cs132.vapor.ast.VLitStr;
import cs132.vapor.ast.VMemRead;
import cs132.vapor.ast.VMemRef.*;
import cs132.vapor.ast.VMemRef.Global;
import cs132.vapor.ast.VMemWrite;
import cs132.vapor.ast.VOperand;
import cs132.vapor.ast.VOperand.Static;
import cs132.vapor.ast.VReturn;
import cs132.vapor.ast.VTarget;
import cs132.vapor.ast.VVarRef;
import cs132.vapor.ast.VVarRef.Local;
import cs132.vapor.ast.VVarRef.Register;
//import cs132.vapor.ast.VMemRef.Stack;

import java.io.*;
import java.util.*;

public class VaporMipsVisitor <E extends Throwable> extends Visitor<E>
{
    StringBuilder mipsCode;

    public void setMipsCode(StringBuilder stringBuilder)
    {
        mipsCode = stringBuilder;
    }

    @Override
    public void visit(VGoto n) throws E //A jump instruction.
    {
        /* public final VAddr<VCodeLabel> target;
        The target of the jump.  
        Can be a direct code label reference or a variable/register.
        */

        /*
        j label   #  jump
            jump to label
        */

        mipsCode.append("\t\t" + "j " + n.target.toString().replace(":", "") + "\n");
    }

    @Override
    public void visit(VBranch n) throws E //A branch instruction (if and if0).
    {
        /*
        boolean positive
            For if branches, positive is true.
        VLabelRef<VCodeLabel> target 
            The code label that will be executed next if the branch is taken.
        VOperand value
            The value being branched on.
        */

        /*
        beqz reg, label   # branch if equal to zero
            branch to label if the value reg is zero
        bnez reg, label   # branch if not equal to zero
            branch to label if the value reg is not zero
        */

        String instruction = (n.positive) ? "bnez " : "beqz " ;

        mipsCode.append("\t\t" + instruction + n.value.toString() + " " + n.target.toString().replace(":", "") + "\n");
    }

    @Override
    public void visit(VCall n) throws E //Function call instruction.
    {
        /*
        The address of the function being called.
        public final VAddr<VFunction> addr;
        An address reference.
        Can either be a label reference VAddr.Label, which statically resolves to something,
        or a VAddr.Var which is a variable/register operand that resolves to a value at runtime
        (a value which may or may not be an address).
        */

        /*
        jal label   # jump and link
            copy program counter (return address) to register $ra
            jump to program statement at label
        jalr reg   # jump to reg and link
            copy program counter (return address) to register $ra
            jump to program statement at the address in reg
        */

        String instruction;

        if (n.addr instanceof Label)
            instruction = "jal ";
        else if (n.addr instanceof Var) 
            instruction = "jalr ";
        else
            instruction = "";

        mipsCode.append("\t\t" + instruction + n.addr.toString().replace(":", "") + "\n");
    }

    @Override
    public void visit(VReturn n) throws E //Return instruction.
    {
        /*
        $v0: returning a result from a call
        From MoreThan4.vaporm:
            $v0 = $t7
            $v0 = 0
        VOperand value
            The value being returned.
        */

        if (n.value == null)
            return;

        String instruction;

        if (n.value instanceof VVarRef)
            instruction = "move";
        else if (n.value instanceof VLitInt) 
            instruction = "li";
        else
            instruction = "";

        mipsCode.append("\t\t" + instruction + " $v0 " + n.value.toString()  + "\n");
    }

    @Override
    public void visit(VAssign n) throws E //An assignment instruction. 
    {
        //This is only used for assignments of simple operands to registers and local variables.

        /*
        public final VVarRef dest
            The location being stored to.
        public final VOperand source
            The value being stored.
        */

        /*
        public class VLitInt extends VOperand.Static
            Integer literal.
        public class VLabelRef<T extends VTarget> extends VOperand.Static
            A label reference. Can be a reference to either a VFunction, a VDataSegment, or a VCodeLabel.
        public abstract class VVarRef extends VOperand
            A reference to a function-local variable (VVarRef.Local) or global register (VVarRef.Register).
        */

        /*
        li $s1, 5   # load immediate
            load immediate value into destination register
            
        la reg, label   # load address
            load address of the label to the register
            
        move reg1, reg2
            copy the value of reg1 to reg2
        */

        String instruction;

        if (n.source instanceof VLitInt)
            instruction = "li ";
        else if (n.source instanceof VLabelRef)
            instruction = "la ";
        else if (n.source instanceof VVarRef)
            instruction = "move ";
        else
            instruction = "";

        mipsCode.append("\t\t" + instruction + n.dest.toString() + " " + n.source.toString().replace(":", "") + "\n");
    }

    @Override
    public void visit(VBuiltIn n) throws E //The invocation of a built-in operation (for primitive operations like add, sub, etc).
    {
        /*
        VOperand[]	args
            The arguments to the operation
        VVarRef	dest
            The variable/register in which to store the result of the operation.
        VBuiltIn.Op	op
            The operation being performed.
        */

        /*
        Basic Arithmetic: Add, Sub, MulS
        Comparison: Eq, Lt, LtS
        Displaying Output: PrintIntS
        Memory Allocation: HeapAllocZ
        Error: Error
        */

        /*
        name
            public final String nameThe name of the operation. This is the name that appears in a Vapor source file.
        Add
            public static final VBuiltIn.Op Add
        Sub
            public static final VBuiltIn.Op Sub
        MulS
            public static final VBuiltIn.Op MulS
        Eq
            public static final VBuiltIn.Op Eq
        Lt
            public static final VBuiltIn.Op Lt
        LtS
            public static final VBuiltIn.Op LtS
        PrintIntS
            public static final VBuiltIn.Op PrintIntS
        Error
            public static final VBuiltIn.Op Error
        HeapAllocZ
            public static final VBuiltIn.Op HeapAllocZ
        */

        String instruction;
        int result = 0;

        switch (n.op.name) 
        {
            case "Add":
            {
                /*
                $t1 = Add($t1 1)
                    addu $t1 $t1 1
                $t3 = Add($t3 $t2)
                    addu $t3 $t3 $t2
                */

                instruction = "addu " + n.dest.toString() + " " + n.args[0].toString() + " " + n.args[1].toString();

                break;
            }

            case "Sub":
            {
                /*
                $t4 = Sub(1 $t4)
                    li $t9 1
                    subu $t4 $t9 $t4
                $t3 = Sub($t1 3)
                    subu $t3 $t1 3
                $s3 = Sub(0 1)
                    li $s3 -1
                */

                if (n.args[0] instanceof VLitInt && !(n.args[1] instanceof VLitInt))
                {
                    instruction = "li $t9 " + n.args[0].toString() + "\n";
                    instruction += "\t\tsubu " + n.dest.toString() + " $t9 " + n.args[1].toString();
                }
                else if (n.args[0] instanceof VLitInt && n.args[1] instanceof VLitInt)
                {
                    result = Integer.parseInt(n.args[0].toString()) - Integer.parseInt(n.args[1].toString());
                    instruction = "li " + n.dest.toString() + " " + Integer.toString(result);
                }
                else if (!(n.args[0] instanceof VLitInt) && n.args[1] instanceof VLitInt)
                    instruction = "subu " + n.dest.toString() + " " + n.args[0].toString() + " " + n.args[1].toString();
                else
                    instruction = "";
                
                break;
            }

            case "MulS":
            {
                /*
                $t1 = MulS(0 4)
                    li $t1 0
                $t2 = MulS(2 $t0)
                    mul $t2 $t0 2
                $t3 = MulS($s1 4)
                    mul $t3 $s1 4
                $t1 = MulS($s0 $t3)
                    mul $t1 $s0 $t3
                */

                if (n.args[0] instanceof VLitInt && n.args[1] instanceof VLitInt)
                {
                    result = Integer.parseInt(n.args[0].toString()) * Integer.parseInt(n.args[1].toString());
                    instruction = "li " + n.dest.toString() + " " + Integer.toString(result);
                }
                else if (n.args[0] instanceof VLitInt && !(n.args[1] instanceof VLitInt))
                    instruction = "mul " + n.dest.toString() + " " + n.args[1].toString() + " " + n.args[0].toString();
                else
                    instruction = "mul " + n.dest.toString() + " " + n.args[0].toString() + " " + n.args[1].toString();

                break;
            }

            case "Lt":
            {
                /*
                $t3 = Lt($s1 $t3)
                    sltu $t3 $s1 $t3
                $t1 = Lt(0 $t1)
                    li $t9 0
                    sltu $t1 $t9 $t1
                */

                if (!(n.args[0] instanceof VLitInt) && !(n.args[1] instanceof VLitInt))
                    instruction = "sltu " + n.dest.toString() + " " + n.args[0].toString() + " " + n.args[1].toString();
                else
                {
                    instruction = "li $t9 " + n.args[0].toString() + "\n";
                    instruction += "\t\tsltu " + n.dest.toString() + " $t9 " + n.args[1].toString();
                }

                break;
            }

            case "LtS":
            {
                /*
                $t1 = LtS($t0 $t1)
                    slt $t1 $t0 $t1
                $t0 = LtS($s3 0)
                    slti $t0 $s3 0
                */

                if (!(n.args[0] instanceof VLitInt) && !(n.args[1] instanceof VLitInt))
                    instruction = "slt ";
                else
                    instruction = "slti ";

                instruction += n.dest.toString() + " " + n.args[0].toString() + " " + n.args[1].toString();

                break;
            }

            case "PrintIntS":
            {
                instruction = (n.args[0] instanceof VLitInt) ? "li $a0 " : "move $a0 ";
                instruction += n.args[0].toString() + "\n\t\tjal _print";

                break;
            }

            case "Error":
            {
                instruction = "la $a0 _str0\n\t\tj _error";

                break;
            }

            case "HeapAllocZ":
            {
                /*
                $t0 = HeapAllocZ(12)
                    li $a0 12
                    jal _heapAlloc
                $t1 = HeapAllocZ($t1)
                    move $a0 $t1
                    jal _heapAlloc
                    move $t1 $v0
                */

                instruction = (n.args[0] instanceof VLitInt) ? "li $a0 " : "move $a0 ";
                instruction += n.args[0].toString() + "\n\t\tjal _heapAlloc";
                instruction += "\n\t\tmove " + n.dest.toString() + " $v0";

                break;
            }
        
            default:
                instruction = "";
                break;
        }

        mipsCode.append("\t\t" + instruction + "\n");
    }

    @Override
    public void visit(VMemWrite n) throws E //Memory write instruction. Ex: "[a+4] = v".
    {
        /*
        VMemRef	dest
            The memory location being written to.
            Either Global or Stack
        VOperand source
            The value being written.
            Either VLitInt or VVarRef
        */

        /*
        out[0] = 4
            li $t9 4
            sw $t9 0($sp)
        out[0] = $t3
            sw $t3 0($sp)
        [$t0] = :vmt_MT4
            la $t9 vmt_MT4
            sw $t9 0($t0)
        */

        String destinaton = null;
        String offset = null;
        String instruction = null;
        String source = n.source.toString().replace(":", "");

        if (n.dest instanceof VMemRef.Stack)
        {
            VMemRef.Stack stack = (VMemRef.Stack) n.dest;

            destinaton = "$sp";
            offset = Integer.toString(stack.index*4);
        }
        else
        {
            VMemRef.Global global = (VMemRef.Global) n.dest;

            destinaton = global.base.toString();
            offset = Integer.toString(global.byteOffset);
        }

        if (n.source instanceof VLitInt)
        {
            instruction = "li $t9 " + source + "\n";
            instruction += "\t\tsw $t9 " + offset + "(" + destinaton + ")";
        }
        else if (n.source instanceof VLabelRef)
        {
            instruction = "la $t9 " + source + "\n";
            instruction += "\t\tsw $t9 " + offset + "(" + destinaton + ")";
        }
        else if (n.source instanceof VVarRef)
            instruction = "sw " + source + " " + offset + "(" + destinaton + ")";
        else
            instruction = "";

        mipsCode.append("\t\t" + instruction + "\n");
    }

    @Override
    public void visit(VMemRead n) throws E //Memory read instructions. Ex: "v = [a+4]"
    {
        /*
        VVarRef	dest
            The variable/register to store the value into.
        VMemRef	source
            The memory location being read.
        */

        /*
        $t1 = [$t0]
            lw $t1 0($t0)
        $s0 = local[0]
            lw $s0 0($sp)
        $t4 = in[0]
            lw $t4 0($fp)
        */

        String destinaton = n.dest.toString().replace(":", "");
        String source = null;
        String offset = null;
        String instruction = null;
        
        if (n.source instanceof VMemRef.Stack)
        {
            VMemRef.Stack stack = (VMemRef.Stack) n.source;

            source = (stack.region == VMemRef.Stack.Region.In) ? "$fp" : "$sp";
            offset = Integer.toString(stack.index*4);
        }
        else
        {
            VMemRef.Global global = (VMemRef.Global) n.source;

            source = global.base.toString();
            offset = Integer.toString(global.byteOffset);
        }

        instruction = "lw " + destinaton + " " + offset + "(" + source + ")";

        mipsCode.append("\t\t" + instruction + "\n");
    }
}
