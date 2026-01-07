assign(
  selected_int_refs:
    %{
      socket.assigns.selected_int_refs.result
      | values: new_int_ref_values
    }
    |> Phoenix.LiveView.AsyncResult.ok()
)
