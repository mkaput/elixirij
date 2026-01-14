@spec maybe_delete_existing_suggestion_and_task(Multi.t(), Suggestion.t() | nil) ::
        {Multi.t(), String.t() | nil}
def maybe_delete_existing_suggestion_and_task(_, _), do: :ok
