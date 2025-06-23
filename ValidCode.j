.class public ValidCode
.super java/lang/Object

.method public <init>()V
   aload_0
   invokespecial java/lang/Object/<init>()V
   return
.end method

.method public static main([Ljava/lang/String;)V
   .limit stack 8
   .limit locals 3
   new Factorial
   dup
   invokespecial Factorial/<init>()V
   astore_1
   aload_1
   ldc 5
   invokevirtual Factorial/calculate(I)I
   istore_2
   getstatic java/lang/System/out Ljava/io/PrintStream;
   iload_2
   invokevirtual java/io/PrintStream/println(I)V
   return
.end method
