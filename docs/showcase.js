const CONFIG = [
  { name: "hello_world", title: "hello_world.lang24" },
  { name: "heapsort", title: "heapsort.lang24" },
  { name: "dom", title: "dom_manipulation.lang24" },
];

const LANG24_SPECS = {
  keywords: [
    "return",
    "if",
    "then",
    "else",
    "while",
    "sizeof",
    "and",
    "not",
    "or",
    "none",
    "nil",
    "true",
    "false",
  ],
  types: ["int", "char", "bool", "void"],
  symbols: /[(){}\[\].,:;=+\-*/%^<>!|]+/g,
};

class Lang24Showcase {
  constructor() {
    this.container = document.getElementById("programs-container");
    this.init();
  }

  async init() {
    for (const prog of CONFIG) {
      await this.renderProgram(prog);
    }
  }

  highlight(code) {
    // First, escape the raw HTML symbols
    let html = code
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;");

    // Define our patterns
    const patterns = [
      { class: "hl-cm", reg: /#.*/g }, // Comments
      { class: "hl-st", reg: /"(?:[^"\\]|\\.)*"/g }, // Strings
      { class: "hl-st", reg: /'(?:[^'\\]|\\.)*'/g }, // Char literals
      {
        class: "hl-kw",
        reg: /\b(return|if|then|else|while|sizeof|and|not|or|none|nil|true|false)\b/g,
      },
      { class: "hl-type", reg: /\b(int|char|bool|void)\b/g },
      { class: "hl-nm", reg: /\b\d+\b/g },
      { class: "hl-sym", reg: /[(){}\[\].,:;=+\-*/%^<>!|]+/g }, // Symbols
    ];

    // Create a single "Master Regex" by joining all patterns with OR (|)
    // We wrap each pattern in a capturing group () so we know which one matched
    const masterReg = new RegExp(
      patterns.map((p) => `(${p.reg.source})`).join("|"),
      "g",
    );

    // Single-pass replacement
    return html.replace(masterReg, (match, ...groups) => {
      // Find which group in the regex matched the text
      const index = groups.findIndex((m) => m === match);
      const type = patterns[index];

      if (type) {
        return `<span class="${type.class}">${match}</span>`;
      }
      return match;
    });
  } // Bridge for reading 8-byte padded strings from Wasm memory
  readLang24String(memory, ptr) {
    const view = new DataView(memory.buffer);
    let str = "";
    let offset = Number(ptr);
    while (true) {
      const charCode = view.getUint8(offset);
      if (charCode === 0) break;
      str += String.fromCharCode(charCode);
      offset += 8; // Pointer increment matches compiler sizeof(char)
    }
    return str;
  }

  getImports(consoleEl) {
    return {
      env: {
        putchar: (c) => (consoleEl.innerText += String.fromCharCode(Number(c))),
        putint: (i) => (consoleEl.innerText += i.toString()),
        exit: (code) => (consoleEl.innerText += `\n[Exit ${code}]`),
      },
      math: {
        random: () => BigInt(Math.floor(Math.random() * 0x7fffffff)),
      },
      dom: {
        // Custom DOM extension
        set_content: (idPtr, contentPtr) => {
          const id = this.readLang24String(this.instance.exports.memory, idPtr);
          const content = this.readLang24String(
            this.instance.exports.memory,
            contentPtr,
          );
          console.log("ID: " + id);
          console.log("contn: " + content);
          const el = document.getElementById(id);
          if (el) el.innerHTML = content;
        },
      },
    };
  }

  async renderProgram(prog) {
    const win = document.createElement("div");
    win.className = "window";
    win.innerHTML = `
            <div class="title-bar">
                <div class="dot red"></div><div class="dot yellow"></div><div class="dot green"></div>
                <div class="title">${prog.title}</div>
            </div>
            <div class="code-area">
                <button class="run-btn" id="btn-${prog.name}">Run Program</button>
                <pre><code id="code-${prog.name}">Loading source...</code></pre>
            </div>
            <div class="console" id="con-${prog.name}">> Waiting for execution...</div>
        `;
    this.container.appendChild(win);

    // Fetch source
    try {
      const srcRes = await fetch(`src/programs/${prog.name}.lang24`);
      const srcText = await srcRes.text();
      document.getElementById(`code-${prog.name}`).innerHTML =
        this.highlight(srcText);
    } catch (e) {
      document.getElementById(`code-${prog.name}`).innerText =
        "Could not load source file.";
    }

    // Setup Run trigger
    document.getElementById(`btn-${prog.name}`).onclick = () =>
      this.runWasm(prog.name);
  }

  async runWasm(name) {
    const consoleEl = document.getElementById(`con-${name}`);
    const btn = document.getElementById(`btn-${name}`);
    consoleEl.innerText = "";
    btn.disabled = true;

    try {
      const response = await fetch(`compiled/${name}.wasm`);
      const buffer = await response.arrayBuffer();
      const result = await WebAssembly.instantiate(
        buffer,
        this.getImports(consoleEl),
      );
      this.instance = result.instance;

      // Execute entry point
      const resultValue = this.instance.exports.main();
      consoleEl.innerText += `\n\n--- Finished with result: ${resultValue} ---`;
    } catch (e) {
      consoleEl.style.color = "#ff5f56";
      consoleEl.innerText = `[Runtime Error] ${e.message}`;
    } finally {
      btn.disabled = false;
    }
  }
}

// Start the showcase
window.addEventListener("DOMContentLoaded", () => new Lang24Showcase());
