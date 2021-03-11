.data
	vmt_LS:
		LS.Start
		LS.Print
		LS.Search
		LS.Init

.text
	jal Main
	li $v0 10	# syscall: exit
	syscall

	Main:
		sw $fp -8($sp)	# save $fp,  2 * 4 = 8
		move $fp $sp
		subu $sp $sp 8
		sw $ra -4($fp)	# save the return address   1*4 = 4
		li $a0 12
		jal _heapAlloc
		la $t9 vmt_LS
		sw $t9 0($t0)
		bnez $t0 null1
		la $a0 _str0
		j _error
		lw $t1 0($t0)
		lw $t1 0($t1)
		move $a0 $t0
		li $a1 10
		jalr $t1
		move $t1 $v0
		move $a0 $t1
		jal _print
		lw $ra -4($fp)              # restore the return address     1 * 4 = 4
		lw $fp -8($fp)              # restore $fp     2 * 4 = 8
		addu $sp $sp 8
		jr $ra                         # return

	LS.Start:
		sw $fp -8($sp)	# save $fp,  2 * 4 = 8
		move $fp $sp
		subu $sp $sp 12
		sw $ra -4($fp)	# save the return address   1*4 = 4
		sw $s0 0($sp)
		move $s0 $a0
		move $t0 $a1
		lw $t1 0($s0)
		lw $t1 12($t1)
		move $a0 $s0
		move $a1 $t0
		jalr $t1
		lw $t1 0($s0)
		lw $t1 4($t1)
		move $a0 $s0
		jalr $t1
		li $a0 9999
		jal _print
		lw $t1 0($s0)
		lw $t1 8($t1)
		move $a0 $s0
		li $a1 8
		jalr $t1
		move $t1 $v0
		move $a0 $t1
		jal _print
		lw $t1 0($s0)
		lw $t1 8($t1)
		move $a0 $s0
		li $a1 12
		jalr $t1
		move $t1 $v0
		move $a0 $t1
		jal _print
		lw $t1 0($s0)
		lw $t1 8($t1)
		move $a0 $s0
		li $a1 17
		jalr $t1
		move $t1 $v0
		move $a0 $t1
		jal _print
		lw $t1 0($s0)
		lw $t1 8($t1)
		move $a0 $s0
		li $a1 50
		jalr $t1
		move $t1 $v0
		move $a0 $t1
		jal _print
		li $v0 55
		lw $s0 0($sp)
		lw $ra -4($fp)              # restore the return address     1 * 4 = 4
		lw $fp -8($fp)              # restore $fp     2 * 4 = 8
		addu $sp $sp 12
		jr $ra                         # return

	LS.Print:
		sw $fp -8($sp)	# save $fp,  2 * 4 = 8
		move $fp $sp
		subu $sp $sp 8
		sw $ra -4($fp)	# save the return address   1*4 = 4
		move $t0 $a0
		li $t1 1
		lw $t2 8($t0)
		slt $t2 $t1 $t2
		beqz $t2 while1_end
		lw $t2 4($t0)
		bnez $t2 null2
		la $a0 _str0
		j _error
		lw $t3 0($t2)
		sltu $t3 $t1 $t3
		bnez $t3 bounds1
		la $a0 _str0
		j _error
		mul $t3 $t1 4
		addu $t3 $t3 $t2
		lw $t3 4($t3)
		move $a0 $t3
		jal _print
		addu $t1 $t1 1
		j while1_top
		li $v0 0
		lw $ra -4($fp)              # restore the return address     1 * 4 = 4
		lw $fp -8($fp)              # restore $fp     2 * 4 = 8
		addu $sp $sp 8
		jr $ra                         # return

	LS.Search:
		sw $fp -8($sp)	# save $fp,  2 * 4 = 8
		move $fp $sp
		subu $sp $sp 8
		sw $ra -4($fp)	# save the return address   1*4 = 4
		move $t0 $a0
		move $t1 $a1
		li $t2 1
		li $t3 0
		lw $t4 8($t0)
		slt $t4 $t2 $t4
		beqz $t4 while2_end
		lw $t4 4($t0)
		bnez $t4 null3
		la $a0 _str0
		j _error
		lw $t5 0($t4)
		sltu $t5 $t2 $t5
		bnez $t5 bounds2
		la $a0 _str0
		j _error
		mul $t5 $t2 4
		addu $t5 $t5 $t4
		lw $t5 4($t5)
		addu $t4 $t1 1
		slt $t6 $t5 $t1
		beqz $t6 if1_else
		j if1_end
		slt $t4 $t5 $t4
		li $t9 1
		subu $t4 $t9 $t4
		beqz $t4 if2_else
		j if2_end
		li $t3 1
		lw $t2 8($t0)
		addu $t2 $t2 1
		j while2_top
		move $v0 $t3
		lw $ra -4($fp)              # restore the return address     1 * 4 = 4
		lw $fp -8($fp)              # restore $fp     2 * 4 = 8
		addu $sp $sp 8
		jr $ra                         # return

	LS.Init:
		sw $fp -8($sp)	# save $fp,  2 * 4 = 8
		move $fp $sp
		subu $sp $sp 12
		sw $ra -4($fp)	# save the return address   1*4 = 4
		sw $s0 0($sp)
		move $s0 $a0
		move $t0 $a1
		sw $t0 8($s0)
		move $a0 $t0
		jal AllocArray
		move $t0 $v0
		sw $t0 4($s0)
		li $t0 1
		lw $t1 8($s0)
		addu $t1 $t1 1
		lw $t2 8($s0)
		slt $t2 $t0 $t2
		beqz $t2 while3_end
		mul $t2 $t0 2
		subu $t3$t1 3
		lw $t4 4($s0)
		bnez $t4 null4
		la $a0 _str0
		j _error
		lw $t5 0($t4)
		sltu $t5 $t0 $t5
		bnez $t5 bounds3
		la $a0 _str0
		j _error
		mul $t5 $t0 4
		addu $t5 $t5 $t4
		addu $t3 $t2 $t3
		sw $t3 4($t5)
		addu $t0 $t0 1
		subu $t1$t1 1
		j while3_top
		li $v0 0
		lw $s0 0($sp)
		lw $ra -4($fp)              # restore the return address     1 * 4 = 4
		lw $fp -8($fp)              # restore $fp     2 * 4 = 8
		addu $sp $sp 12
		jr $ra                         # return

	AllocArray:
		sw $fp -8($sp)	# save $fp,  2 * 4 = 8
		move $fp $sp
		subu $sp $sp 8
		sw $ra -4($fp)	# save the return address   1*4 = 4
		move $t0 $a0
		mul $t1 $t0 4
		addu $t1 $t1 4
		move $a0 $t1
		jal _heapAlloc
		sw $t0 0($t1)
		move $v0 $t1
		lw $ra -4($fp)              # restore the return address     1 * 4 = 4
		lw $fp -8($fp)              # restore $fp     2 * 4 = 8
		addu $sp $sp 8
		jr $ra                         # return

	_print:
		li $v0 1	# syscall: print integer
		syscall
		la $a0 _newline	#address of string in memory
		li $v0 4	# syscall: print string
		syscall
		jr $ra

	_error:
		li $v0 4	# syscall: print string
		syscall
		li $v0 10	# syscall: exit
		syscall

	_heapAlloc:
		li $v0 9	# syscall: sbrk
		syscall	# address in $v0
		jr $ra

	.data
	.align 0
		_newline: .asciiz "\n"
		_str0: .asciiz "null pointer\n"
