use lambda_runtime::{handler_fn, Context};
use serde_json::{Value, json};

type Error = Box<dyn std::error::Error + Send + Sync + 'static>;

#[tokio::main]
async fn main() -> Result<(), Error> {
  let func = handler_fn(func);

  lambda_runtime::run(func).await?;

  Ok(())
}

async fn func(event: Value, _: Context) -> Result<String, Error> {
  Ok("Hello world!".to_string())
}
