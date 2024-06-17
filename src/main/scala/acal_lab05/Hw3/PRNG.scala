package acal_lab05.Hw3

import chisel3._
import chisel3.util._
object LfsrTaps {
    def apply(size: Int): Seq[Int] = {
        size match {
            // Seqp[Int] means the taps in LFSR
            case 4 => Seq(3)          //p(x) = x^4+x^3+1
            case 8 => Seq(6,5,4)      //p(x) = x^8+x^6+x^5+x^4+1
            case 16 => Seq(16,14,13,11)
            case _ => throw new Exception("No LFSR taps stored for requested size")
        }
    }
}
class PRNG(seed:Int) extends Module{
    val io = IO(new Bundle{
        val gen = Input(Bool())
        val puzzle = Output(Vec(4,UInt(4.W)))
        val ready = Output(Bool())
    })
    val sIdle :: sWait :: sCompu :: sCheck :: sOut :: Nil = Enum(5)
    io.puzzle := VecInit(Seq.fill(4)(0.U(4.W)))
    val pWire = Wire(Vec(4, UInt(4.W)))
    pWire := VecInit(Seq.fill(4)(0.U(4.W)))
    io.ready:=false.B
    val checkTable = RegInit(VecInit(Seq.fill(10000)(false.B)))
    

    val gen = RegNext(io.gen)

    val n = 16
    val shiftReg = RegInit(VecInit(Seq.fill(n)(false.B)))
    val PassReg = RegInit(0.U(2.W))

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
        state := sWait
    }
    }

    //Output Decoder
    switch(state){
    is(sWait){
        io.ready := false.B
        PassReg:=1.U
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
        }//check是否和前面出現過的重複
        when(checkTable(io.puzzle(0)*1000.U+io.puzzle(1)*100.U+io.puzzle(2)*10.U+io.puzzle(3))){PassReg:=2.U }
        checkTable(io.puzzle(0)*1000.U+io.puzzle(1)*100.U+io.puzzle(2)*10.U+io.puzzle(3)) := true.B//設為true
    }
    is(sOut){
        io.ready := true.B
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

}
