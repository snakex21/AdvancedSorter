# Advanced Sorter (Minecraft 1.12.2)

[PL] **Advanced Sorter** to mod do Minecrafta 1.12.2 skupiający się na zaawansowanej logistyce przedmiotów i płynów. Oferuje wydajne rury teleportacyjne, systemy sortowania, inteligentny kontroler pomp, automatyczny crafter z własnymi recepturami oraz zbiorniki na ciecze z konfigurowalnymi stronami.

[EN] **Advanced Sorter** is a Minecraft 1.12.2 mod focused on advanced item and fluid logistics. It offers efficient teleport pipes, sorting systems, an intelligent pump controller, an auto crafter with custom recipes, and fluid tanks with configurable sides.

---

## 🚀 Features / Funkcje

### [PL] Polskie Funkcje:
- **Rury Teleportacyjne**: Przesyłaj przedmioty, płyny i gazy (Mekanism) na dowolne odległości bez kabli.
- **Kontroler Pomp z Presetami**: Zarządzaj wieloma częstotliwościami pomp z jednego miejsca. Możliwość nazywania i zapisywania do 6 ustawień (presetów).
- **Zaawansowane Sortowanie**: Precyzyjne filtrowanie i kierowanie ruchem przedmiotów w Twojej bazie.
- **Auto Crafter**: Automatyczny crafter z własnymi recepturami:
  - Definiuj własne receptury (mapowanie składników -> wynik)
  - Łańcuchowe craftowanie (jeśli brakuje składnika, craftuje go najpierw)
  - Tryb Priorytetowy lub Round-Robin dla automatyzacji
  - Integracja z rurami (góra = input, boki/dół = output)
- **Inventory Index**: Centralny system zarządzania skrzynkami - przeglądaj zawartość wielu skrzynek z jednego miejsca.
- **Zbiorniki na Ciecze (NOWOŚĆ v2.0)**:
  - 4 tiery: Basic (16B), Advanced (64B), Elite (256B), Ultimate (1024B)
  - Konfigurowalne strony: INPUT, OUTPUT lub wyłączone
  - Jednoczesne przyjmowanie i wysyłanie cieczy (jak Gas Tank z Mekanism)
  - Obsługa wiader (wlewanie/wylewanie)
  - Widoczna ciecz wewnątrz zbiornika przez przezroczyste szkło
  - Wizualne oznaczenie podpiętych rur w GUI (zielona ramka)
  - Zachowanie cieczy przy zbieraniu bloku

### [EN] English Features:
- **Teleport Pipes**: Transport items, fluids, and gases (Mekanism) over any distance without physical connections.
- **Pump Controller with Presets**: Manage multiple pump frequencies from a single block. Name and save up to 6 custom settings (presets).
- **Advanced Sorting**: Precise filtering and routing of items throughout your base.
- **Auto Crafter**: Automatic crafter with custom recipes:
  - Define custom recipes (ingredient mapping -> result)
  - Chain crafting (if an ingredient is missing, crafts it first)
  - Priority or Round-Robin mode for automation
  - Pipe integration (top = input, sides/bottom = output)
- **Inventory Index**: Central chest management system - browse contents of multiple chests from one place.
- **Fluid Tanks (NEW in v2.0)**:
  - 4 tiers: Basic (16B), Advanced (64B), Elite (256B), Ultimate (1024B)
  - Configurable sides: INPUT, OUTPUT, or disabled
  - Simultaneous input and output (like Mekanism's Gas Tank)
  - Bucket support (fill/drain with buckets)
  - Visible fluid inside the tank through transparent glass
  - Visual indicator for connected pipes in GUI (green border)
  - Fluid preservation when block is picked up

---

## 📦 Crafting Recipes / Receptury

### Fluid Tanks / Zbiorniki na Ciecze

| Tier | Recipe / Receptura |
|------|-------------------|
| Basic | 8x Iron Ingot + Glass |
| Advanced | 8x Gold Ingot + Basic Tank |
| Elite | 8x Diamond + Advanced Tank |
| Ultimate | 8x Emerald + Elite Tank |

---

## 🔧 Changelog / Historia zmian

### v2.0.0
- **Nowe zbiorniki na ciecze** - 4 tiery z konfigurowalnymi stronami (INPUT/OUTPUT)
- **Obsługa wiader** - wlewanie i wylewanie cieczy prawym przyciskiem
- **Wizualizacja połączeń** - zielona ramka w GUI pokazuje podpięte rury
- **Optymalizacja Smart Sync** - częsta synchronizacja cieczy tylko przy otwartym GUI (oszczędność sieci)
- **Optymalizacja CPU** - Tick Skipping (wysyłanie co 5 ticków) i Cache'owanie Sąsiadów (redukcja lagów przy dużej liczbie zbiorników)
- **Poprawka teleport pipes** - naprawiono problem z niedziałającymi rurami po załadowaniu świata

### v1.0.0
- Pierwsza wersja z rurami teleportacyjnymi, kontrolerem pomp, auto crafterem i inventory index

---

## 🛠 Installation / Instalacja

1. **Forge**: Required / Wymagany (1.12.2).
2. **Download**: Get the latest `.jar` from the `build/libs` folder.
3. **Place**: Drop the file into your `mods` folder.

---

## 💻 Development / Rozwój

[PL] Projekt oparty na Gradle. Aby zbudować moda samodzielnie:
[EN] Project based on Gradle. To build the mod yourself:

```bash
./gradlew build
```

---

## 📄 License / Licencja
MIT License
