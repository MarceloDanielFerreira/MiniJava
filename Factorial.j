.class public Factorial
.super java/lang/Object

.method public <init>()V
   aload_0
   invokespecial java/lang/Object/<init>()V
   return
.end method


.method public calculate(I)I
   .limit stack 8
   .limit locals 4
   ldc 1
   istore_2
   ldc 1
   istore_3
LabelWhileStart1996181658:
   iload_3
   iload_1
   ldc 1
   iadd
   if_icmplt LabelLtTrue806353501
   ldc 0
   goto LabelLtEnd806353501
LabelLtTrue806353501:
   ldc 1
LabelLtEnd806353501:
   ifeq LabelWhileEnd1996181658
   iload_2
   iload_3
   imul
   istore_2
   iload_3
   ldc 1
   iadd
   istore_3
   goto LabelWhileStart1996181658
LabelWhileEnd1996181658:
   iload_2
   ireturn
.end method
