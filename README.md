# Dictionary Client Application
---

## 🧰 Features

- Connects to DICT servers over TCP (port 2628)
- Lists available databases and matching strategies
- Performs word definition queries (`DEFINE`)
- Supports partial matches with different strategies (`MATCH`)
- Parses and validates multi-line DICT responses

---

## ⚙️ How It Works

Implemented in `DictionaryConnection.java`:
- **Socket Communication**: Opens a TCP connection to the DICT server and sends commands per [RFC 2229](https://datatracker.ietf.org/doc/html/rfc2229).
- **Command Support**: Includes `SHOW DB`, `SHOW STRAT`, `DEFINE`, `MATCH`, and `QUIT`.
- **Response Parsing**: Handles protocol-specific replies, parses definitions, and throws errors on malformed responses.
- **GUI Integration**: The rest of the code connects the network logic to a Java Swing interface for user interaction.

---

### 📄 License

This repository contains the code for a client application that connects to a DICT.org server. The project was developed as part of CPSC 317 Course at the University of British Columbia (UBC).
Only `DictionaryConnection.java and DictionaryMain.java was implemented by me. All other code was provided by the UBC Computer Science department.
This code is provided as-is for educational purposes. All non-authored files belong to their respective creators within the UBC Computer Science department.
