import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

import org.openrndr.math.Vector2
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.random.Random

enum class GameState {
    STOPPED, RUNNING
}

fun Vector2.angle(): Double {
    val rawAngle = atan2(y = this.y, x = this.x)
    return (rawAngle / Math.PI) * 180
}

class Game {
    var prevTime = 0L
    val ship = ShipData()
    var puntos=0
    var balas=3
    var repeat=2
    var targetLocation by mutableStateOf<DpOffset>(DpOffset.Zero)

    var gameObjects = mutableStateListOf<GameObject>()
    var gameState by mutableStateOf(GameState.RUNNING)
    var gameStatus by mutableStateOf("Let's play!")
    var gameLifes by mutableStateOf("  vidas: "+ship.vidas)

    fun startGame() {
        gameObjects.clear()
        ship.vidas=3
        puntos=0
        repeat=2
        gameLifes="  vidas: "+ship.vidas
        ship.position = Vector2(width.value / 2.0, height.value / 2.0)
        ship.movementVector = Vector2.ZERO
        gameObjects.add(ship)
        repeat(3) {
            gameObjects.add(AsteroidData().apply {
                position = Vector2(100.0, 100.0); angle = Random.nextDouble() * 360.0; speed = 2.0
            })
        }
        gameState = GameState.RUNNING
        gameStatus = "Good luck!"
    }
    fun startGameHard(){
        gameObjects.clear()
        ship.vidas=2
        puntos=0
        repeat=2
        gameLifes="  vidas: "+ship.vidas
        ship.position = Vector2(width.value / 2.0, height.value / 2.0)
        ship.movementVector = Vector2.ZERO
        gameObjects.add(ship)
        repeat(4) {
            gameObjects.add(AsteroidData().apply {
                position = Vector2(100.0, 100.0); angle = Random.nextDouble() * 360.0; speed = 2.0
            })
        }
        gameObjects.add(AsteroideData().apply {
            position = Vector2(100.0, 400.0); angle = Random.nextDouble() * 360.0; speed = 2.0})
        gameState = GameState.RUNNING
        gameStatus = "Good luck!"
    }

    fun update(time: Long) {

        val delta = time - prevTime
        val floatDelta = (delta / 1E8).toFloat()
        prevTime = time

        if (gameState == GameState.STOPPED) return

        val cursorVector = Vector2(targetLocation.x.value.toDouble(), targetLocation.y.value.toDouble())
        val shipToCursor = cursorVector - ship.position
        var angle = atan2(y = shipToCursor.y, x = shipToCursor.x)

    /*    Modifier.onKeyEvent {
            if (it.key == Key.W) {
                ship.visualAngle += 10
                ship.movementVector = ship.movementVector + (shipToCursor.normalized * floatDelta.toDouble())
                println("w")
            }else if(it.key == Key.S){
                ship.visualAngle -= 10
                ship.movementVector = ship.movementVector + (shipToCursor.normalized * floatDelta.toDouble())
                println("s")
            }
            false
        }*/



        ship.visualAngle = shipToCursor.angle()

        ship.movementVector = ship.movementVector + (shipToCursor.normalized * floatDelta.toDouble())

        for (gameObject in gameObjects) {
            gameObject.update(floatDelta, this)
        }
        if (puntos>=50){
            balas=4
            repeat=4
        }
        if (puntos>=75){
            balas=5
            repeat=5
        }
        val bullets = gameObjects.filterIsInstance<BulletData>()

        // Limit number of bullets at the same time
        if (bullets.count() > balas) {
            gameObjects.remove(bullets.first())
        }
        val asteroids = gameObjects.filterIsInstance<AsteroidData>()
        val asteroides = gameObjects.filterIsInstance<AsteroideData>()

        // Bullet <-> Asteroid interaction
        asteroids.forEach { asteroid ->
            val least = bullets.firstOrNull { it.overlapsWith(asteroid) } ?: return@forEach
            if (asteroid.position.distanceTo(least.position) < asteroid.size) {
                gameObjects.remove(asteroid)
                gameObjects.remove(least)
                puntos++
                if (asteroid.size < 50.0) return@forEach
                // it's still pretty big, let's spawn some smaller ones
                repeat(2) {
                    gameObjects.add(AsteroidData(asteroid.speed * 2,
                        Random.nextDouble() * 360.0,
                        asteroid.position).apply {
                        size = asteroid.size / 2
                    })
                }
            }
        }

        // Asteroid <-> Ship interaction
        if(asteroides.isNotEmpty()){
            asteroides.forEach {     asteroide ->
                val least = bullets.firstOrNull { it.overlapsWith(asteroide) } ?: return@forEach
                if (asteroide.position.distanceTo(least.position) < asteroide.size) {
                    gameObjects.remove(asteroide)
                    gameObjects.remove(least)
                    puntos++
                    if (asteroide.size < 50.0) return@forEach
                    // it's still pretty big, let's spawn some smaller ones
                    repeat(2) {
                        gameObjects.add(AsteroideData(asteroide.speed * 2,
                            Random.nextDouble() * 360.0,
                            asteroide.position).apply {
                            size = asteroide.size / 2
                        })
                    }
                }
            }
        }
        if (asteroides.any { asteroide -> ship.overlapsWith(asteroide)}) {
            ship.vidas--
            gameLifes="  vidas: "+ship.vidas
            ship.position=Vector2(700.0,700.0)

            if(ship.vidas<=0) {
                endGame()
            }
        }
        if(asteroids.any { asteroid -> ship.overlapsWith(asteroid)}){
            ship.vidas--
            gameLifes="  vidas: "+ship.vidas
            ship.position=Vector2(700.0,700.0)

            if(ship.vidas<=0) {
                endGame()
            }
        }

        if (asteroides.isEmpty() && asteroids.isEmpty()) {
            gameObjects.add(AsteroideData().apply {
                position = Vector2(100.0, 400.0); angle = Random.nextDouble() * 360.0; speed = 2.0})
            repeat(repeat){
            gameObjects.add(AsteroidData().apply {
                position = Vector2(100.0, 400.0); angle = Random.nextDouble() * 360.0; speed = 2.0})}

        }
    }


    fun endGame() {
        gameObjects.remove(ship)
        gameState = GameState.STOPPED
        gameStatus = "Better luck next time!"
    }


    var width by mutableStateOf(0.dp)
    var height by mutableStateOf(0.dp)
}