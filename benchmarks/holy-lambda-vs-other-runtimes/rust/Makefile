build: target/x86_64-unknown-linux-musl/release/hello_world
	@echo "Building Rust artifacts"
	@cp target/x86_64-unknown-linux-musl/release/hello_world ./hello_world && zip latest-rust.zip bootstrap hello_world && rm hello_world
	@mv latest-rust.zip ../artifacts/

target/x86_64-unknown-linux-musl/release/hello_world: src/main.rs
	@cargo build --release --target x86_64-unknown-linux-musl

