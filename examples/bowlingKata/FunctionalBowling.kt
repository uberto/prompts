data class GameState(val rolls: List<Int> = listOf())

fun roll(gameState: GameState, pins: Int): GameState = GameState(gameState.rolls + pins)

fun score(gameState: GameState): Int = calculateScore(gameState.rolls, 0, 0)

private tailrec fun calculateScore(rolls: List<Int>, frameIndex: Int, frame: Int): Int =
    if (frame == 10) 0
    else when {
        isStrike(rolls, frameIndex) -> 10 + strikeBonus(rolls, frameIndex) + calculateScore(rolls, frameIndex + 1, frame + 1)
        isSpare(rolls, frameIndex) -> 10 + spareBonus(rolls, frameIndex) + calculateScore(rolls, frameIndex + 2, frame + 1)
        else -> sumOfBallsInFrame(rolls, frameIndex) + calculateScore(rolls, frameIndex + 2, frame + 1)
    }


fun calcBowlingScore(rolls: List<Pins>): Int {

    fun getRoll(roll: Int): Int = rolls.getOrElse(roll){ Pins.zero }.number

    fun isStrike(frameIndex: Int): Boolean =
        getRoll(frameIndex) == 10
    fun sumOfBallsInFrame(frameIndex: Int): Int =
        getRoll(frameIndex) + getRoll(frameIndex + 1)
    fun spareBonus(frameIndex: Int): Int =
        getRoll(frameIndex + 2)
    fun strikeBonus(frameIndex: Int): Int =
        getRoll(frameIndex + 1) + getRoll(frameIndex + 2)
    fun isSpare(frameIndex: Int): Boolean =
        getRoll(frameIndex) + getRoll(frameIndex + 1) == 10

    var score = 0
    var frameIndex = 0
    for (frame in 0..9) {
        if (isStrike(frameIndex)) {
            score += 10 + strikeBonus(frameIndex)
            frameIndex++
        } else if (isSpare(frameIndex)) {
            score += 10 + spareBonus(frameIndex)
            frameIndex += 2
        } else {
            score += sumOfBallsInFrame(frameIndex)
            frameIndex += 2
        }
    }
    return score
}



private fun isStrike(rolls: List<Int>, frameIndex: Int): Boolean = rolls.getOrNull(frameIndex) == 10

private fun sumOfBallsInFrame(rolls: List<Int>, frameIndex: Int): Int =
    rolls.getOrNull(frameIndex).orZero() + rolls.getOrNull(frameIndex + 1).orZero()

private fun spareBonus(rolls: List<Int>, frameIndex: Int): Int = rolls.getOrNull(frameIndex + 2).orZero()

private fun strikeBonus(rolls: List<Int>, frameIndex: Int): Int =
    rolls.getOrNull(frameIndex + 1).orZero() + rolls.getOrNull(frameIndex + 2).orZero()

private fun isSpare(rolls: List<Int>, frameIndex: Int): Boolean =
    rolls.getOrNull(frameIndex).orZero() + rolls.getOrNull(frameIndex + 1).orZero() == 10

private fun Int?.orZero(): Int = this ?: 0
