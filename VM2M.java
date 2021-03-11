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
import cs132.vapor.ast.VMemRef;
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

public class VM2M 
{
    public static void main(String[] args) 
    {
        try 
        {
            VaporProgram vaporProgramTree = parseVapor(System.in, System.err);

            if (vaporProgramTree == null)
                return;

            /* print data section
            .data
                var1: .word 5
                table:
                    Label1
                    Label2
            */
            System.out.println(".data");
            for (int i = 0; i < vaporProgramTree.dataSegments.length; i++) 
            {
                String varTable  = vaporProgramTree.dataSegments[i].ident;
                System.out.println("\t" + varTable + ":");

                for (int j = 0; j < vaporProgramTree.dataSegments[i].values.length; j++) 
                {
                    String Label = vaporProgramTree.dataSegments[i].values[j].toString().replace(":", "");
                    System.out.println("\t\t" + Label);          
                }
            }
            System.out.println();

            /*
            .text
                jal Main
                li $v0 10   # syscall: exit
                syscall
            */
            System.out.println(".text");
            System.out.println("\tjal Main");
            System.out.println("\tli $v0 10\t# syscall: exit");
            System.out.println("\tsyscall\n");

            /*
                Main:
                    ...
            */
            VaporMipsVisitor<Exception> vaporMvisitor = new VaporMipsVisitor<>();
            for (int i = 0; i < vaporProgramTree.functions.length; i++) 
            {
                VFunction vaporFunction = vaporProgramTree.functions[i];

                System.out.println("\t" + vaporFunction.ident + ":");

                int stackFrameSize = (vaporFunction.stack.out * 4) + (vaporFunction.stack.local * 4) + 8;

                pushFrame(stackFrameSize);

                StringBuilder mipsCode = new StringBuilder();
                vaporMvisitor.setMipsCode(mipsCode);

                for (int j = 0; j < vaporFunction.body.length; j++) 
                {
                    vaporFunction.body[j].accept(vaporMvisitor);
                }

                System.out.print(mipsCode.toString());

                popFrame(stackFrameSize);

                System.out.println();
            }

            /*
            _print:
                li $v0 1   # syscall: print integer
                syscall
                la $a0 _newline   # address of string in memory
                li $v0 4   # syscall: print string
                syscall
                jr $ra
            _error:
                li $v0 4   # syscall: print string
                syscall
                li $v0 10  # syscall: exit
                syscall
            _heapAlloc:
                li $v0 9   # syscall: sbrk
                syscall    # address in $v0
                jr $ra
            .data
            .align 0
                _newline: .asciiz "\n"
                _str0: .asciiz "null pointer\n"
            */
            System.out.println("\t_print:");
            System.out.println("\t\tli $v0 1\t# syscall: print integer");
            System.out.println("\t\tsyscall");
            System.out.println("\t\tla $a0 _newline\t#address of string in memory");
            System.out.println("\t\tli $v0 4\t# syscall: print string");
            System.out.println("\t\tsyscall");
            System.out.println("\t\tjr $ra\n");

            System.out.println("\t_error:");
            System.out.println("\t\tli $v0 4\t# syscall: print string");
            System.out.println("\t\tsyscall");
            System.out.println("\t\tli $v0 10\t# syscall: exit");
            System.out.println("\t\tsyscall\n");

            System.out.println("\t_heapAlloc:");
            System.out.println("\t\tli $v0 9\t# syscall: sbrk");
            System.out.println("\t\tsyscall\t# address in $v0");
            System.out.println("\t\tjr $ra\n");

            System.out.println("\t.data");
            System.out.println("\t.align 0");
            System.out.println("\t\t_newline: .asciiz \"\\n\"");
            System.out.println("\t\t_str0: .asciiz \"null pointer\\n\"");
        } 
        catch (Exception e) 
        {
            System.out.println(e.toString());
            e.printStackTrace();
        }
    }

    public static VaporProgram parseVapor(InputStream in, PrintStream err) throws IOException 
    {
        Op[] ops = { Op.Add, Op.Sub, Op.MulS, Op.Eq, Op.Lt, Op.LtS, Op.PrintIntS, Op.HeapAllocZ, Op.Error };
      
        boolean allowLocals = false;
        String[] registers = 
        {
            "v0", "v1",
            "a0", "a1", "a2", "a3",
            "t0", "t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8",
            "s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7"
        };
        boolean allowStack = true;
      
        VaporProgram tree;
        try 
        {
            tree = VaporParser.run(new InputStreamReader(in), 1, 1, java.util.Arrays.asList(ops), allowLocals, registers, allowStack);
        }
        catch (ProblemException ex) 
        {
            err.println(ex.getMessage());
            return null;
        }
        
        return tree;
    }

    public static void pushFrame(int stackFrameSize)
    {
        /*
        # 1. Pushing the frame
        sw $fp,-8($sp)               # save $fp,  2 * 4 = 8
        move $sp, $fp
        subu $sp,$sp,36      # push stack frame, (2+3+2)*4 = 36
        sw $ra,-4($fp)               # save the return address   1*4 = 4
        */
        System.out.println("\t\tsw $fp -8($sp)\t# save $fp,  2 * 4 = 8");
        System.out.println("\t\tmove $fp $sp");
        System.out.println("\t\tsubu $sp $sp " + stackFrameSize);
        System.out.println("\t\tsw $ra -4($fp)\t# save the return address   1*4 = 4");
    }

    public static void popFrame(int stackFrameSize)
    {
        /*
        # 2. Popping the frame
        lw $ra,-4($fp)              # restore the return address     1 * 4 = 4
        lw $fp,-8($fp)              # restore $fp     2 * 4 = 8
        addu $sp,$sp,36      # pop stack frame, (2+3+2)*4 = 36
    
        jr $ra                         # return
        */
        System.out.println("\t\tlw $ra -4($fp)              # restore the return address     1 * 4 = 4");
        System.out.println("\t\tlw $fp -8($fp)              # restore $fp     2 * 4 = 8");
        System.out.println("\t\taddu $sp $sp " + stackFrameSize);
        System.out.println("\t\tjr $ra                         # return");
    }
}
