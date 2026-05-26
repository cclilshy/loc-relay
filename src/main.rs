mod cli;
mod endpoint;
mod frp;
mod model;
mod output;
mod paths;
mod service;
mod state;

pub(crate) type Result<T> = std::result::Result<T, Box<dyn std::error::Error>>;

fn main() {
    if let Err(err) = cli::run() {
        eprintln!("ERROR: {err}");
        std::process::exit(1);
    }
}
