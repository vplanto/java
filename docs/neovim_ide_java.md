# 🚀 Neovim Ultimate Setup (C++, Java, Python) на Ubuntu

Цей гайд дозволяє розгорнути професійне середовище розробки на чистій Ubuntu 20.04/22.04/24.04.
**Включено:** NvChad, LSP (автодоповнення), Formatter (автоформатування), Debugger.

## 1\. Підготовка системи (Terminal)

Встановлюємо всі необхідні компілятори та інструменти однією командою.

```bash
# Оновлюємо списки пакетів
sudo apt update

# 1. Базові інструменти та компілятори (C/C++)
sudo apt install -y curl git gcc g++ make unzip ripgrep

# 2. Залежності для Python (venv + pip)
sudo apt install -y python3 python3-venv python3-pip

# 3. Залежності для Java (JDK 21 або 17)
sudo apt install -y openjdk-21-jdk

# 4. Залежності для Node.js (потрібен для роботи LSP серверів)
sudo apt install -y nodejs npm
```

## 2\. Встановлення Neovim (AppImage)

Використовуємо AppImage, щоб отримати найсвіжішу версію (в apt репозиторіях вона часто стара).

```bash
cd ~
# Завантажуємо останню версію
curl -LO https://github.com/neovim/neovim/releases/latest/download/nvim.appimage

# Робимо файл виконуваним та переміщуємо у системну папку
chmod u+x nvim.appimage
sudo mv nvim.appimage /usr/local/bin/nvim

# Перевірка
nvim --version
# Має бути v0.10.0 або вище
```

## 3\. Шрифт (Nerd Fonts)

Критично для відображення іконок.

```bash
mkdir -p ~/.local/share/fonts
cd ~/.local/share/fonts 
curl -fLo "JetBrainsMono.zip" https://github.com/ryanoasis/nerd-fonts/releases/latest/download/JetBrainsMono.zip
unzip -o JetBrainsMono.zip
rm JetBrainsMono.zip
fc-cache -f -v
```

> **УВАГА:** Після цього відкрийте налаштування терміналу -\> Preferences -\> Profile -\> Text -\> Custom Font і виберіть **JetBrainsMono Nerd Font**.

## 4\. Встановлення NvChad (База)

```bash
# Видаляємо старі конфіги якщо є
rm -rf ~/.config/nvim ~/.local/share/nvim ~/.cache/nvim

# Клонуємо NvChad
git clone https://github.com/NvChad/NvChad ~/.config/nvim --depth 1

# Створюємо папку для наших налаштувань
mkdir -p ~/.config/nvim/lua/custom/configs
```

## 5\. Файли конфігурації (Copy & Paste)

Просто виконуйте ці блоки коду в терміналі. Вони створять правильні файли.

### A. `init.lua` (З виправленням помилок)

Цей файл запускає Neovim і **блокує набридливі Warning повідомлення** про застарілі функції.

```bash
cat > ~/.config/nvim/init.lua << 'EOF'
-- 1. FIX: Polyfill для застарілої функції get_active_clients
if vim.lsp.get_clients then vim.lsp.get_active_clients = vim.lsp.get_clients end

-- 2. FIX: Приховуємо повідомлення "sign_define is deprecated"
local orig_notify = vim.notify
vim.notify = function(msg, level, opts)
  -- Якщо повідомлення містить "sign_define", ми його ігноруємо
  if type(msg) == "string" and (msg:find("sign_define") or msg:find("deprecated")) then
    return
  end
  orig_notify(msg, level, opts)
end

require "core"

local custom_init_path = vim.api.nvim_get_runtime_file("lua/custom/init.lua", false)[1]

if custom_init_path then
  dofile(custom_init_path)
end

require("core.utils").load_mappings()

local lazypath = vim.fn.stdpath "data" .. "/lazy/lazy.nvim"

-- bootstrap lazy.nvim!
if not vim.loop.fs_stat(lazypath) then
  require("core.bootstrap").gen_chadrc_template()
  require("core.bootstrap").lazy(lazypath)
end

dofile(vim.g.base46_cache .. "defaults")
vim.opt.rtp:prepend(lazypath)
require "plugins"
EOF
```

### B. `plugins.lua` (Інструменти)

Список того, що Mason встановить автоматично.

```bash
cat > ~/.config/nvim/lua/custom/plugins.lua << 'EOF'
local plugins = {
  {
    "nvimtools/none-ls.nvim",
    event = "VeryLazy",
    opts = function()
      return require "custom.configs.null-ls"
    end,
  },
  {
    "neovim/nvim-lspconfig",
    config = function()
      require "plugins.configs.lspconfig"
      require "custom.configs.lspconfig"
    end,
  },
  {
    "williamboman/mason.nvim",
    opts = {
      ensure_installed = {
        -- C++
        "clangd", "clang-format", "codelldb",
        -- Python
        "pyright", "black",
        -- Java
        "jdtls", "google-java-format",
      }
    }
  }
}
return plugins
EOF
```

### C. `lspconfig.lua` (Налаштування мов)

Містить критичні виправлення для Java (шляхи та пошук кореня проєкту).

```bash
cat > ~/.config/nvim/lua/custom/configs/lspconfig.lua << 'EOF'
local base = require("plugins.configs.lspconfig")
local on_attach = base.on_attach
local capabilities = base.capabilities

local lspconfig = require("lspconfig")
local util = require("lspconfig/util")

-- Шлях до інструментів Mason
local mason_bin = vim.fn.stdpath("data") .. "/mason/bin"

-- === C++ ===
lspconfig.clangd.setup {
  on_attach = function(client, bufnr)
    client.server_capabilities.signatureHelpProvider = false
    on_attach(client, bufnr)
  end,
  capabilities = capabilities,
}

-- === Python ===
lspconfig.pyright.setup {
  on_attach = on_attach,
  capabilities = capabilities,
  filetypes = { "python" },
}

-- === Java ===
-- Використовуємо прямий шлях до скрипта запуску
local jdtls_cmd = mason_bin .. "/jdtls"

lspconfig.jdtls.setup {
  cmd = { jdtls_cmd },
  on_attach = on_attach,
  capabilities = capabilities,
  filetypes = { "java" },
  -- Визначаємо корінь проєкту (де лежить .git або build файл)
  root_dir = function(fname)
    return util.root_pattern(".git", "mvnw", "gradlew", "pom.xml")(fname) or vim.fn.getcwd()
  end,
}
EOF
```

### D. `null-ls.lua` (Автоформатування)

Налаштовує форматування коду при натисканні `Ctrl+S` (або `:w`).

```bash
cat > ~/.config/nvim/lua/custom/configs/null-ls.lua << 'EOF'
local augroup = vim.api.nvim_create_augroup("LspFormatting", {})
local null_ls = require("null-ls")

local opts = {
  sources = {
    null_ls.builtins.formatting.clang_format,       -- C++
    null_ls.builtins.formatting.black,              -- Python
    null_ls.builtins.formatting.google_java_format, -- Java
  },
  on_attach = function(client, bufnr)
    if client.supports_method("textDocument/formatting") then
      vim.api.nvim_clear_autocmds({ group = augroup, buffer = bufnr })
      vim.api.nvim_create_autocmd("BufWritePre", {
        group = augroup,
        buffer = bufnr,
        callback = function()
          vim.lsp.buf.format({ bufnr = bufnr })
        end,
      })
    end
  end,
}
return opts
EOF
```

## 6\. Перший запуск та ініціалізація

1.  Запустіть Neovim: `nvim`
2.  **Нічого не робіть 1-2 хвилини.**
      * Ви побачите, як Lazy завантажує плагіни.
      * Ви побачите, як Mason встановлює сервери (`clangd`, `jdtls`...).
3.  Якщо Mason не почав встановлення сам, введіть: `:MasonInstallAll`.
4.  Коли все завершиться — перезапустіть Neovim.

## 7\. Робота з Java (Важливо\!)

Java сервер (jdtls) вимагає, щоб ваш код був у "проєкті". Якщо ви просто створите файл `Test.java` на робочому столі, сервер може не запуститися.

**Правильний спосіб почати роботу з Java:**

```bash
mkdir my_java_project
cd my_java_project
git init            # <--- ЦЕ МАГІЧНА КОМАНДА (створює контекст проєкту)
nvim Main.java
```

Після цього `jdtls` запуститься коректно.