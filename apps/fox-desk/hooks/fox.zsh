# Fox shell hook: after each command, tells Fox Desk which folder you are in. Nothing else is sent:
# no command text, no output, no environment. Fox Desk only looks at folders on its allowlist.
# Add to ~/.zshrc:  source "/path/to/apps/fox-desk/hooks/fox.zsh"

zmodload -F zsh/datetime p:EPOCHSECONDS 2>/dev/null

_fox_desk_precmd() {
    local sock="$HOME/Library/Application Support/FoxDesk/desk.sock"
    [[ -S "$sock" ]] || return 0
    # Control characters cannot be sent safely as JSON text; skip such folders rather than send broken JSON.
    [[ "$PWD" == *[[:cntrl:]]* ]] && return 0
    local cwd=${PWD//\\/\\\\}      # escape backslashes first
    cwd=${cwd//\"/\\\"}            # then quotes
    local payload="{\"cwd\":\"$cwd\",\"ts\":${EPOCHSECONDS:-0}}"
    # Fire and forget in the background, so the prompt never waits on Fox Desk.
    ( print -rn -- "$payload" | /usr/bin/nc -U -w 1 "$sock" >/dev/null 2>&1 & ) >/dev/null 2>&1
}

autoload -Uz add-zsh-hook
add-zsh-hook precmd _fox_desk_precmd
