package com.example.data.model

import androidx.annotation.StringRes
import com.example.R

data class GameTheme(
    val id: String,
    @param:StringRes val nameRes: Int,
    val category: String,
    @param:StringRes val descriptionRes: Int,
    val priceCoins: Int,
    val symbols: List<Pair<String, String>>, // Symbol and display name
    val primaryColorHex: Long = 0xFF7209B7,
    val cardBgColorHex: Long = 0xFF2D225A,
    val isDefaultUnlocked: Boolean = false
) {
    companion object {
        val ANIMALS = GameTheme(
            id = "animals",
            nameRes = R.string.theme_animals_name,
            category = "Fauna",
            descriptionRes = R.string.theme_animals_desc,
            priceCoins = 0,
            isDefaultUnlocked = true,
            symbols = listOf(
                "🐶" to "Cachorro", "🐱" to "Gato", "🦊" to "Raposa", "🐻" to "Urso",
                "🐼" to "Panda", "🦁" to "Leão", "🐯" to "Tigre", "🐸" to "Sapo",
                "🐵" to "Macaco", "🦄" to "Unicórnio", "🦉" to "Coruja", "🐙" to "Polvo",
                "🐬" to "Golfinho", "🐘" to "Elefante", "🦒" to "Girafa", "🦋" to "Borboleta",
                "🐝" to "Abelha", "🦩" to "Flamingo", "🦔" to "Oriço", "🦥" to "Preguiça",
                "🐧" to "Pinguim", "🦘" to "Canguru", "🦚" to "Pavão", "🐺" to "Lobo",
                "🦝" to "Guaxinim", "🦦" to "Lutra", "🦨" to "Gambá", "🦡" to "Teixugo",
                "🐊" to "Crocodilo", "🐢" to "Tartaruga", "🦖" to "T-Rex", "🦈" to "Tubarão"
            )
        )

        val FRUITS = GameTheme(
            id = "fruits",
            nameRes = R.string.theme_fruits_name,
            category = "Natureza",
            descriptionRes = R.string.theme_fruits_desc,
            priceCoins = 150,
            symbols = listOf(
                "🍎" to "Maçã", "🍌" to "Banana", "🍉" to "Melancia", "🍇" to "Uva",
                "🍓" to "Morango", "🍒" to "Cereja", "🍍" to "Abacaxi", "🥝" to "Kiwi",
                "🍑" to "Pêssego", "🥑" to "Abacate", "🥭" to "Manga", "🍊" to "Laranja",
                "🍋" to "Limão", "🍐" to "Pêra", "🥥" to "Coco", "🫐" to "Mirtilo",
                "🍈" to "Melão", "🍅" to "Tomate", "🌽" to "Milho", "🥕" to "Cenoura",
                "🌶️" to "Pimenta", "🥦" to "Brócolis", "🍄" to "Cogumelo", "🥜" to "Amendoim",
                "🌰" to "Castanha", "🥐" to "Croissant", "🥨" to "Pretzel", "🥞" to "Panqueca",
                "🧇" to "Waffle", "🧀" to "Queijo", "🍖" to "Carne", "🍗" to "Frango"
            )
        )

        val CARS = GameTheme(
            id = "cars",
            nameRes = R.string.theme_cars_name,
            category = "Transporte",
            descriptionRes = R.string.theme_cars_desc,
            priceCoins = 250,
            symbols = listOf(
                "🚗" to "Carro", "🏎️" to "Fórmula 1", "🚀" to "Foguete", "🚁" to "Helicóptero",
                "⛵" to "Veleiro", "🚂" to "Trem", "🚜" to "Trator", "🚲" to "Bicicleta",
                "🛵" to "Motoneta", "✈️" to "Avião", "🛸" to "OVNI", "🚑" to "Ambulância",
                "🚒" to "Bombeiro", "🚓" to "Polícia", "🚕" to "Táxi", "🚌" to "Ônibus",
                "🚚" to "Caminhão", "🏍️" to "Moto", "Sub" to "Submarino", "🛺" to "Riquixá",
                "🛶" to "Canoa", "🚢" to "Navio", "🚆" to "Metrô", "Cable" to "Bonde",
                "🛸" to "Espaçonave", "🏎️" to "Carro Esporte", "🚚" to "Carreta", "🛩️" to "Jatinho",
                "🛰️" to "Satélite", " Hover" to "Hovercraft", "🚜" to "Escavadeira", "🚚" to "Van"
            )
        )

        val FOOD = GameTheme(
            id = "food",
            nameRes = R.string.theme_food_name,
            category = "Culinária",
            descriptionRes = R.string.theme_food_desc,
            priceCoins = 300,
            symbols = listOf(
                "🍕" to "Pizza", "🍔" to "Hambúrguer", "🌮" to "Taco", "🍩" to "Rosca",
                "🍦" to "Sorvete", "🍣" to "Sushi", "🍟" to "Batata", "🍿" to "Pipoca",
                "🥐" to "Croissant", "🍰" to "Bolo", "🥞" to "Panquecas", "🥨" to "Pretzel",
                "🥟" to "Guioza", "Bento" to "Marmita", "🌭" to "Cachorro Quente", "🥪" to "Sanduíche",
                "🌯" to "Burrito", "🥙" to "Kebab", "🧆" to "Falafel", "🍳" to "Ovo Frito",
                "🥘" to "Paella", "🍲" to "Sopa", "🥗" to "Salada", "🫕" to "Fondue",
                "🍝" to "Espaguete", "🍜" to "Lamen", "🍠" to "Batata Doce", "🍢" to "Espetinho",
                "🍡" to "Dango", "🍧" to "Raspadinha", "🍨" to "Sorvete Taça", "🧁" to "Cupcake"
            )
        )

        val SPACE = GameTheme(
            id = "space",
            nameRes = R.string.theme_space_name,
            category = "Cosmos",
            descriptionRes = R.string.theme_space_desc,
            priceCoins = 400,
            symbols = listOf(
                "🪐" to "Saturno", "🚀" to "Foguete", "🌟" to "Estrela", "☄️" to "Cometa",
                "🛸" to "Disco Voador", "🌕" to "Lua Cheia", "👨‍🚀" to "Astronauta", "🛰️" to "Satélite",
                "🌌" to "Galáxia", "👾" to "Alienígena", "☀️" to "Sol", "🌍" to "Terra",
                "🔭" to "Telescópio", "🌠" to "Estrela Cadente", "🌑" to "Lua Nova", "🪐" to "Júpiter",
                "👽" to "ET", "🛰️" to "Sonda", "🌌" to "Nebulosa", "👨‍🚀" to "Cosmonauta",
                "🌟" to "Supernova", "☄️" to "Meteoro", "🚀" to "Lançador", "🛸" to "Nave",
                "🪐" to "Urano", "🪐" to "Netuno", "🌑" to "Eclipse", "✨" to "Poeira Estelar",
                "🌌" to "Via Láctea", "👨‍🚀" to "Passeio Espacial", "🛰️" to "ISS", "👾" to "Invader"
            )
        )

        val DINOSAURS = GameTheme(
            id = "dinosaurs",
            nameRes = R.string.theme_dinosaurs_name,
            category = "Pré-História",
            descriptionRes = R.string.theme_dinosaurs_desc,
            priceCoins = 500,
            symbols = listOf(
                "🦖" to "T-Rex", "🦕" to "Brontossauro", "🐊" to "Jacaré", "🦎" to "Lagarto",
                "🥚" to "Ovo Dino", "🌋" to "Vulcão", "🦴" to "Fóssil", "🌴" to "Palmeira",
                "🐾" to "Pegada", "☄️" to "Meteoro", "🐍" to "Serpente", "🐢" to "Tartaruga",
                "🪵" to "Tronco", "🪨" to "Rocha", "🌿" to "Folha Pré-histórica", "🦂" to "Escorpião",
                "🕷️" to "Aranha Gigante", "🕸️" to "Teia", "🦣" to "Mamute", "🦤" to "Dodô",
                "🦣" to "Tigre Dente-de-Sabre", "🦖" to "Velociraptor", "🦕" to "Pterodáctilo", "🥚" to "Ninho",
                "🌋" to "Erupção", "🦴" to "Osso", "🦎" to "Triceratops", "🐊" to "Espinossauro",
                "🦖" to "Anquilossauro", "🦕" to "Estegossauro", "🌴" to "Selva Antiga", "🪨" to "Caverna"
            )
        )

        val HOLIDAYS = GameTheme(
            id = "holidays",
            nameRes = R.string.theme_holidays_name,
            category = "Festividades",
            descriptionRes = R.string.theme_holidays_desc,
            priceCoins = 350,
            symbols = listOf(
                "🎄" to "Árvore de Natal", "🎅" to "Papai Noel", "🎁" to "Presente", "⛄" to "Boneco de Neve",
                "🔔" to "Sino", "❄️" to "Floco de Neve", "🦌" to "Rena", "🕯️" to "Vela",
                "🎆" to "Fogos de Artifício", "🎃" to "Abóbora", "👻" to "Fantasma", "🎉" to "Confete",
                "🎈" to "Balão", "🎊" to "Festim", "🎀" to "Laço", "🎇" to "Estrelinha",
                "🧧" to "Envelope Vermelho", "🏮" to "Lanterna", "🥮" to "Bolo da Lua", "🦃" to "Peru",
                "🥚" to "Ovo de Páscoa", "🐇" to "Coelhinho", "💐" to "Buquê", "👑" to "Coroa", "🎭" to "Carnaval"
            )
        )

        val EMOJIS = GameTheme(
            id = "emojis",
            nameRes = R.string.theme_emojis_name,
            category = "Divertido",
            descriptionRes = R.string.theme_emojis_desc,
            priceCoins = 200,
            symbols = listOf(
                "😎" to "Estiloso", "🤩" to "Maravilhado", "🥳" to "Festeiro", "🤖" to "Robô",
                "🤠" to "Cowboy", "👻" to "Fantasma", "👽" to "Alien", "🤡" to "Palhaço",
                "😍" to "Apaixonado", "🤑" to "Rico", "🤓" to "Nerd", "🤯" to "Explodindo",
                "💩" to "Cocozinho", "😜" to "Piscando", "😇" to "Anjo", "😈" to "Diabinho",
                "🙈" to "Tampando Olhos", "🙉" to "Tampando Ouvidos", "🙊" to "Tampando Boca", "🔥" to "Fogo",
                "💎" to "Diamante", "⭐" to "Estrela", "⚡" to "Raio", "🎯" to "Alvo"
            )
        )

        val ALL_THEMES = listOf(
            ANIMALS, FRUITS, CARS, FOOD, SPACE, DINOSAURS, HOLIDAYS, EMOJIS
        )
    }
}
