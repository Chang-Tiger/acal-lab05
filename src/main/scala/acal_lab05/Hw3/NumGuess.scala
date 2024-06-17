package acal_lab05.Hw3

import chisel3._
import chisel3.util._

class NumGuess(seed:Int = 1) extends Module{
    require (seed > 0 , "Seed cannot be 0")

    val io  = IO(new Bundle{
        val gen = Input(Bool())
        val guess = Input(UInt(16.W))
        val puzzle = Output(Vec(4,UInt(4.W)))
        val ready  = Output(Bool())
        val g_valid  = Output(Bool())
        val A      = Output(UInt(3.W))
        val B      = Output(UInt(3.W))
     

        //don't care at Hw6-3-2 but should be considered at Bonus
        val s_valid = Input(Bool())
    })
    io.puzzle := VecInit(Seq.fill(4)(0.U(4.W)))
    io.ready  := false.B
    io.g_valid  := false.B
    io.A      := 0.U
    io.B      := 0.U

    val sIdle :: sWait :: sCompu :: sCheck :: sOut :: sGuess :: Nil = Enum(6)
    
    val pWire = Wire(Vec(4, UInt(4.W)))
    pWire := VecInit(Seq.fill(4)(0.U(4.W)))
    io.ready:=false.B
    val checkTable = RegInit(VecInit(Seq.fill(10000)(false.B)))
    

    val gen = RegNext(io.gen)

    val n = 16
    val shiftReg = RegInit(VecInit(Seq.fill(n)(false.B)))
    val PassReg = RegInit(0.U(2.W))
    val GuessReg = RegInit(0.U(3.W))
    val state = RegInit(sIdle)
    
    //Next State Decoder
    switch(state){
    is(sIdle){
        shiftReg zip seed.asUInt.asBools map {case(l,r) => l := r}
        io.ready:=false.B
        state := sWait
    }
    is(sWait){//等待訊號
        when(io.gen){
           state := sCompu
        }
    }
    is(sCompu){//計算
        state := sCheck
    }
    is(sCheck){//檢查重複問題
        when(PassReg === 0.U){//去output
            state := sOut
        }.elsewhen(PassReg === 2.U){//回去產生新數
            state := sCompu
        }
    }
    is(sOut){
        //state := sWait
        state := sGuess
    }
    is(sGuess){//sGuess state根據io.A訊號判定完成或是要重猜，重猜回sOut
        when(GuessReg === 5.U){
            state := state
        }.elsewhen(GuessReg === 4.U){
            state := sWait
        }.otherwise{
            state := sOut
        }
    }
    }

    //Output Decoder
    switch(state){
    is(sWait){
        io.ready := false.B
        PassReg:=1.U
        GuessReg:= 5.U
    }
    is(sCompu){
        when(gen || PassReg === 2.U){
            (shiftReg.zipWithIndex).map{
            case(sr,i) => sr := shiftReg((i+1)%n)
            }
            //Fibonacci LFSR
            shiftReg(n-1) := (LfsrTaps(n).map(x=>shiftReg(n-x)).reduce(_^_))
        }
        
        PassReg:=1.U
    }
    is(sCheck){
        PassReg:=0.U//是否有重複數字
        for(i <- 0 until 3){
            for(j <- i+1 until 4){
                when(io.puzzle(i) === io.puzzle(j)){PassReg:=2.U}
            }
        }//check是否和前面出現過的重複(1A2B game不需要)
        //when(checkTable(io.puzzle(0)*1000.U+io.puzzle(1)*100.U+io.puzzle(2)*10.U+io.puzzle(3))){PassReg:=2.U }
        //checkTable(io.puzzle(0)*1000.U+io.puzzle(1)*100.U+io.puzzle(2)*10.U+io.puzzle(3)) := true.B//設為true
    }
    is(sOut){
        io.ready := true.B
        GuessReg := 5.U
    }
    is(sGuess){//output guess result
        GuessReg := io.A
        io.g_valid:=true.B
        //io.A:=4.U
    }
    }

    val p0_ = Cat(shiftReg(3), shiftReg(2), shiftReg(1), shiftReg(0)).asUInt
    val p1_ = Cat(shiftReg(7), shiftReg(6), shiftReg(5), shiftReg(4)).asUInt
    val p2_ = Cat(shiftReg(11), shiftReg(10), shiftReg(9), shiftReg(8)).asUInt
    val p3_ = Cat(shiftReg(15), shiftReg(14), shiftReg(13), shiftReg(12)).asUInt
    
    pWire(0) := Mux(p0_ > 9.U, p0_ - 10.U, p0_)
    pWire(1) := Mux(p1_ > 9.U, p1_ - 10.U, p1_)
    pWire(2) := Mux(p2_ > 9.U, p2_ - 10.U, p2_)
    pWire(3) := Mux(p3_ > 9.U, p3_ - 10.U, p3_)
    
    io.puzzle := pWire
    //Both position & number correct
    val n0 = Mux(io.guess(3,0) === io.puzzle(0),1.U(3.W),0.U(3.W))
    val n1 = Mux(io.guess(7,4) === io.puzzle(1),1.U(3.W),0.U(3.W))
    val n2 = Mux(io.guess(11,8) === io.puzzle(2),1.U(3.W),0.U(3.W))
    val n3 = Mux(io.guess(15,12) === io.puzzle(3),1.U(3.W),0.U(3.W))

    io.A:=(n0 + n1 + n2 + n3)
    //數字正確卻不在正確position
    val b0 = (io.guess(3,0) === io.puzzle(1)) ^ (io.guess(3,0) === io.puzzle(2)) ^ (io.guess(3,0) === io.puzzle(3))
    val b1 = (io.guess(7,4) === io.puzzle(0)) ^ (io.guess(7,4) === io.puzzle(2)) ^ (io.guess(7,4) === io.puzzle(3))
    val b2 = (io.guess(11,8) === io.puzzle(0)) ^ (io.guess(11,8) === io.puzzle(1)) ^ (io.guess(11,8) === io.puzzle(3))
    val b3 = (io.guess(15,12) === io.puzzle(0)) ^ (io.guess(15,12) === io.puzzle(1)) ^ (io.guess(15,12) === io.puzzle(2))

    val b0_ = Mux(b0, 1.U(3.W),0.U(3.W))
    val b1_ = Mux(b1, 1.U(3.W),0.U(3.W))
    val b2_ = Mux(b2, 1.U(3.W),0.U(3.W))
    val b3_ = Mux(b3, 1.U(3.W),0.U(3.W))

    io.B:=(b0_ + b1_ + b2_ + b3_)

}