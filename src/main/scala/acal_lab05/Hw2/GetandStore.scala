package acal_lab05.Hw2

import chisel3._
import chisel3.util._


class GetandStore extends Module{
    val io = IO(new Bundle{
        val key_in = Input(UInt(4.W))
        val getData = Output(Valid(UInt(32.W)))
        val is_num = Output(Bool())          
    }) 

    io.getData.valid := false.B
    io.getData.bits := 0.U       
    io.is_num := false.B

    val in_buffer0 = RegNext(io.key_in)
    //type of input key_in
    val isnum = Wire(Bool())
    isnum:= in_buffer0 < 10.U
    val isoperator = Wire(Bool())
    isoperator := in_buffer0 <= 12.U && in_buffer0 >=10.U
    val isleft = Wire(Bool())
    isleft := in_buffer0 === 13.U
    val isright = Wire(Bool())
    isright := in_buffer0 === 14.U
    val isequal = Wire(Bool())
    isequal := in_buffer0 === 15.U

    

    val is_neg = RegInit(false.B) //負號出現,is_neg = !(is neg)


    val sIdle :: sSrc :: sOp :: sLeft :: sRight :: sEqual :: Nil = Enum(6)
    val state = RegInit(sIdle)
    val pre_state = RegNext(state)

    val src = RegInit(0.U(32.W))

    val in_buffer1 = RegNext(in_buffer0)
    val in_buffer2 = RegNext(in_buffer1)


  //Next State Decoder
    switch(state){
        is(sIdle){
            when(isoperator) {state := sLeft}              
            .elsewhen(isnum){ state := sSrc}
            .elsewhen(isleft){ state := sLeft}
        }
        is(sOp){
            when(isnum) {state := sSrc}
            .elsewhen(isleft){state := sLeft}
            .elsewhen(isequal){state := sEqual}    
        }
        is(sLeft){
            when(isnum){state := sSrc}
        }
        is(sSrc){
            when(isoperator){ state := sOp}
            .elsewhen(isequal){ state := sEqual}
            .elsewhen(isright){ state := sRight}
        }
        is(sRight){ 
            when(isoperator){ state:= sOp}
            .elsewhen(isequal){ state := sEqual}
        }
        is(sEqual){ state:=sIdle}
    }

    //Output Decoder
    switch(state){//在這個state output前一個state的data
        is(sIdle){
            when(pre_state === sEqual){
                io.getData.bits := 15.U
                io.getData.valid := true.B
                io.is_num := false.B
            }
        }
        is(sOp){
            when(pre_state === sSrc){
                io.getData.bits := src 
                io.getData.valid := true.B 
                io.is_num := true.B
            }.elsewhen( pre_state === sRight){
                is_neg := false.B
                io.getData.bits := 14.U  
                io.getData.valid := !is_neg 
                io.is_num := false.B
            }.elsewhen(pre_state === sEqual){
                io.getData.bits := 15.U
                io.getData.valid := true.B
                io.is_num := false.B
            }
        }
        is(sLeft){//sLeft時遇到 - ，先不output，並用is_neg紀錄
            when(in_buffer1 === 11.U){
                is_neg := !is_neg
            }.elsewhen(pre_state === sOp){
                io.getData.bits := in_buffer2  
                io.getData.valid := true.B
                io.is_num := false.B
            }.elsewhen(pre_state === sLeft){
                io.getData.bits := 13.U  
                io.getData.valid := true.B
                io.is_num := false.B
            }
            when(pre_state === sEqual){
                io.getData.bits := 15.U
                io.getData.valid := true.B
                io.is_num := false.B
            }
        }
        is(sSrc){
            io.getData.valid := false.B
            when(pre_state === sSrc){
                src := (src<<3.U) + (src<<1.U) + in_buffer1
            }.otherwise{
                src :=  in_buffer1
            }
            when(pre_state === sOp){
                io.getData.bits := in_buffer2  
                io.getData.valid := true.B
                io.is_num := false.B
            }.elsewhen(pre_state === sLeft){
                io.getData.bits := 13.U 
                io.getData.valid := !is_neg
                io.is_num := false.B
            }.elsewhen(pre_state === sEqual){
                io.getData.bits := 15.U
                io.getData.valid := true.B
                io.is_num := false.B
            }
        }
        is(sRight){
            io.getData.bits := Mux(pre_state === sRight, 14.U, Mux(is_neg, (~src).asUInt + 1.U, src ) )//is_neg decide是否取負數output
            io.getData.valid := true.B
            io.is_num := !(pre_state === sRight)
        }
        is(sEqual){
            io.getData.bits := MuxLookup(pre_state , 0.U, Seq(
                sOp -> in_buffer2,
                sLeft -> 13.U,
                sRight -> 14.U,
                sSrc -> src
            ))  
            io.getData.valid := Mux(pre_state === sRight, !is_neg, true.B )
            io.is_num := (pre_state === sSrc)

            when(pre_state =/= sEqual){
                in_buffer1 := 0.U
                in_buffer2 := 0.U
                src := 0.U
                is_neg := false.B 
            }
        }
    }


}