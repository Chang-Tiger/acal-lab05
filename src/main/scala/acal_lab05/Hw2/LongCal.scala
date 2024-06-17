package acal_lab05.Hw2

import chisel3._
import chisel3.util._

class LongCal extends Module{
    val io = IO(new Bundle{
        val key_in = Input(UInt(4.W))
        val value = Output(Valid(UInt(32.W)))
    })

    //please implement your code below

    //Wire Declaration======
    val operator = WireDefault(false.B)
    operator := io.key_in >= 10.U && io.key_in <= 14.U

    val num = WireDefault(false.B)
    num := io.key_in < 10.U

    val equal = WireDefault(false.B)
    equal := io.key_in === 15.U
    
    
    //Reg Declaration============
    val in_buffer = RegNext(io.key_in)
    val in_buffer2 = RegNext(in_buffer)
    val src1 = RegInit(0.U(32.W))
    val op = RegInit(0.U(2.W))
    val src2 = RegInit(0.U(32.W))

    val if_left = RegInit(false.B) //when left bracket appear, if_left = true
    val if_neg = RegInit(false.B) //when minus appear ,if_neg = !(if neg)
    val left_counter = RegInit(0.U(4.W)) 

    //State  Declaration========
    val sIdle :: sSrc1 :: sOp :: sSrc2 :: sEqual :: Nil = Enum(5)
    val add = 1.U
    val sub = 2.U
    val mul = 3.U 
    
    val state = RegInit(sIdle)
    val prestate = RegInit(0.U)
    
    //Next State Decoder
    switch(state){
        is(sIdle){
            when(operator) {state := sOp}
            .elsewhen(equal){state := sEqual}
            .elsewhen(num){ state := sSrc1}
        }
        is(sSrc1){
            when(operator) {state := sOp}
            .elsewhen(equal){state := sEqual}
        }
        is(sOp){
            when(num) {
                state := sSrc2
                prestate := sOp}
            .elsewhen(equal){state := sEqual}
            .elsewhen(operator){ 
                prestate := sOp
            }
            
        }
        is(sSrc2){
            when(equal) {state := sEqual}
            .elsewhen(operator){ 
                state := sOp
                prestate := sSrc2
            }.elsewhen(num){
                prestate :=sSrc2
            }
        }
        is(sEqual){
            state := sSrc1
        }
    }
    //next state decoder end ================
    

    
    val if_src1 = RegInit(false.B)

    when(state === sSrc1){
        src1 := (src1<<3.U) + (src1<<1.U) + in_buffer
        if_src1 := 1.U
        }
    when(state === sSrc2){
        when(prestate === sOp ){ // 加減乘之後的num,之前算的結果存在scr1，scr2要重新賦值
            src1 := Mux(if_src1 , src1, src2) 
            src2 := in_buffer
        }.otherwise{ src2 := (src2<<3.U) + (src2<<1.U) + in_buffer }}
    when(state === sOp){
        
        //為第二個(以上)加減乘發生的情況，要把之前算的結果移到src1。前面不能是' ( '，非運算性質
        when(op =/= 0.U && (in_buffer >=10.U && in_buffer <= 12.U) && in_buffer2 =/= 13.U){ 
            if_src1:= 0.U 
            src2  := MuxLookup(op,0.U,Seq(
            add -> (src1 + src2),
            sub -> (src1 - src2),
            mul -> (src1 * src2)
    ))
        }

        when(in_buffer === 13.U ){ 
            if_src1:= 0.U 
            if_left := 1.U
            left_counter := left_counter + 1.U
        }.elsewhen(in_buffer ===14.U){  //op= ')'
            // if left bracked has appeared, check if_neg = true,and let number become minus_number
            if_src1:= 0.U 
            when(if_left ){
                when(if_neg){ 
                    src2:= ( (~src2).asUInt + 1.U) 
                    if_neg := !(if_neg)
                    }
                left_counter := left_counter - 1.U
                if_left := Mux(left_counter <= 1.U, 0.U , 1.U)
            }
        }.elsewhen( (if_left > 0.U) && (in_buffer === 11.U)){    //op = '-'
            if_src1:= 0.U 
            if_neg := !(if_neg)
        }.otherwise{ op := in_buffer - 9.U} // op為運算性質
    }

    when(state === sEqual){
        src1 := 0.U
        src2 := 0.U
        op := 0.U
        in_buffer := 0.U
        if_left := 0.U
        left_counter := 0.U
        
    }

    io.value.valid := Mux(state === sEqual,true.B,false.B)
    io.value.bits := MuxLookup(op,src2+src1,Seq(
        add -> (src1 + src2),
        sub -> (src1 - src2),
        mul -> (src1 * src2)
    ))


}